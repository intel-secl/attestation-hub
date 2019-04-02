/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.attestationhub.setup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.attestationhub.common.Constants;
import com.intel.mtwilson.configuration.ConfigurationFactory;
import com.intel.mtwilson.configuration.ConfigurationProvider;
import com.intel.mtwilson.setup.AbstractSetupTask;
import com.intel.mtwilson.setup.SetupException;

/**
 * @author Siddharth
 */
public class ApplyDatabasePatches extends AbstractSetupTask {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApplyDatabasePatches.class);
    private static final String CHANGELOG_FILE = "changelog.sql";

    private static String databaseDriver;
    private static String databaseUsername;
    private static String databasePassword;
    private static String databaseUrl;
    private static String databaseVendor;
    private static Connection connection;

    @Override
    protected void configure() throws Exception {
        log.debug("Going to ApplyDatabasePatches");
        File customFile = new File(
                Folders.configuration() + File.separator + Constants.ATTESTATION_HUB_PROPRRTIES_FILE_NAME);
        ConfigurationProvider provider;
        try {
            provider = ConfigurationFactory.createConfigurationProvider(customFile);
            com.intel.dcsg.cpg.configuration.Configuration loadedConfiguration = provider.load();
            databaseDriver = loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_DRIVER);
            databaseUrl = loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_URL);
            databaseUsername = loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_USERNAME);
            databasePassword = loadedConfiguration.get(Constants.ATTESTATION_HUB_DB_PASSWORD);
            databaseVendor = "postgresql";
        } catch (IOException e1) {
            log.error("Failed to fetch database properties from {}", Constants.ATTESTATION_HUB_PROPRRTIES_FILE_NAME,
                    e1);
        }

        if (databaseDriver == null) {
            configuration("Database driver not configured");
        } else {
            log.debug("Database driver: {}", databaseDriver);
        }

        if (databaseUrl == null) {
            configuration("Database URL not configured");
        } else {
            log.debug("Database URL: {}", databaseUrl);
        }

        checkAvailableUpdates();
    }

    @Override
    protected void validate() throws Exception {
        if (testConnection()) {
            checkAvailableUpdates();
        }
    }

    @Override
    protected void execute() throws Exception {
        initDatabase();
    }

    private boolean testConnection() {
        boolean status = false;
        Statement s1 = null;
        ResultSet rs = null;
        try {
            log.debug("Inside  testConnection ");
            getConnection();
            s1 = connection.createStatement();
            rs = s1.executeQuery("SELECT 1");
            status = true;
        } catch (Exception e) {
            log.error("AttestationHub DB Patches : cannot connect to database", e);
            validation("AttestationHub DB Patches : Cannot connect to database");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.error("ApplyDatabasePatches , testConnection : cannot close result set", e);
                }
            }

            if (s1 != null) {
                try {
                    s1.close();
                } catch (SQLException e) {
                    log.error("ApplyDatabasePatches , testConnection : cannot close create statement", e);
                }
            }
            closeConnection();
        }
        return status;
    }

    public static class ChangelogEntry {
        public String id;
        public String applied_at;
        public String description;

        public ChangelogEntry() {
        }

        public ChangelogEntry(String id, String applied_at, String description) {
            this.id = id;
            this.applied_at = applied_at;
            this.description = description;
        }
    }

    private void verbose(String format, Object... args) {
        log.debug(String.format(format, args));
    }

    private Map<Long, Resource> sql;

    private HashSet<Long> fetchChangesToApply() throws SetupException, IOException, SQLException {
        log.debug("Inside fetchChangesToApply() : " + databaseVendor);
        sql = getSql(databaseVendor);

        log.debug("Connecting to {}", databaseVendor);
        try {
            getConnection();
        } catch (SQLException e) {
            log.error("Failed to connect to {} with schema: error = {}", databaseVendor, e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            log.error("Failed to connect to {} with schema: error = {}", databaseVendor, e.getMessage());
            return null;
        }
        List<ChangelogEntry> changelog;

        changelog = getChangelog(connection);
        closeConnection();
        HashMap<Long, ChangelogEntry> presentChanges = new HashMap<>();
        verbose("Existing database changelog has %d entries", changelog.size());
        for (ChangelogEntry entry : changelog) {
            if (entry != null) {
                verbose("%s %s %s", entry.id, entry.applied_at, entry.description);
                presentChanges.put(Long.valueOf(entry.id), entry);
            }
        }

        HashSet<Long> unknownChanges = new HashSet<>(presentChanges.keySet());
        unknownChanges.removeAll(sql.keySet());
        if (unknownChanges.isEmpty()) {
            log.debug("Database is compatible");
        } else {
            log.warn("Database schema is newer than this version of Mt Wilson");
            ArrayList<Long> unknownChangesInOrder = new ArrayList<>(unknownChanges);
            Collections.sort(unknownChangesInOrder);
            for (Long unknownChangeId : unknownChangesInOrder) {
                ChangelogEntry entry = presentChanges.get(unknownChangeId);
                log.info(String.format("%s %s %s", entry.id, entry.applied_at, entry.description));
            }
        }
        HashSet<Long> changesToApply = new HashSet<>(sql.keySet());
        changesToApply.removeAll(presentChanges.keySet());
        return changesToApply;
    }

    private void checkAvailableUpdates() throws SetupException, IOException, SQLException {
        HashSet<Long> changesToApply = fetchChangesToApply();
        if (changesToApply == null) {
            log.info("No database updates available");
            return;
        }

        if (changesToApply.isEmpty()) {
            log.info("No database updates available");
        } else {
            validation("There are %s database updates to apply", changesToApply.size());
        }
    }

    /**
     * Must call checkAvailableUpdates() first in order to initialize the "sql"
     * and "changesToApply" member variables
     *
     * @throws SetupException
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void initDatabase() throws SetupException, IOException, SQLException, ClassNotFoundException {
        HashSet<Long> changesToApply = fetchChangesToApply();
        log.info(" initDatabase  Connecting to {}", databaseVendor);
        ArrayList<Long> changesToApplyInOrder = new ArrayList<>(changesToApply);
        Collections.sort(changesToApplyInOrder);
        ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();

        boolean changeLogTableExist = checkIfTableExist("changelog");
        boolean ahHostTableExist = checkIfTableExist("ah_host");
        log.info("changeLogTableExist::" + changeLogTableExist + " ahHostTableExist::" + ahHostTableExist);
        if (!changeLogTableExist) {
            rdp.addScript(getSqlResource(CHANGELOG_FILE));
            log.debug("changelog table do not exist");
            log.debug("Adding sql script for execution :changelog.sql");
        }

        int startIndex = 0;
        if (ahHostTableExist && !changeLogTableExist) { /// It has GA
            /// database
            log.info("Skipping execution of first sql file i.e bootstrap sql file");
            startIndex = 1;/// Skipping the execution first sql file i.e GA
            /// build file as this condition is only true
            /// when existing database schema is of GA
        }
        for (int i = startIndex; i < changesToApplyInOrder.size(); i++) {
            Long id = changesToApplyInOrder.get(i);
            log.info("Adding sql script for execution id:" + id);
            rdp.addScript(sql.get(id));
        }

        rdp.setContinueOnError(true);
        rdp.setIgnoreFailedDrops(true);
        rdp.setSeparator(";");
        rdp.populate(getConnection());
        closeConnection();
    }

    private Resource getSqlResource(String sqlFileName) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                getClass().getClassLoader());
        Resource resource = resolver.getResource(
                "classpath:com/intel/mtwilson/attestation-hub/database/" + databaseVendor + "/" + sqlFileName);

        return resource;
    }

    /**
     * Locates the SQL files for the specified vendor, and reads them to create
     * a mapping of changelog-date to SQL content. This mapping can then be used
     * to select which files to execute against an existing database. See also
     * iBatis, which we are (very) roughly emulating.
     *
     * @param databaseVendor
     * @return
     */
    private Map<Long, Resource> getSql(String databaseVendor) throws SetupException {
        log.debug("Scanning for " + databaseVendor + " SQL files");
        HashMap<Long, Resource> sqlmap = new HashMap<>();
        try {
            Resource[] list = listResources(databaseVendor);
            log.debug("Got list of sql files");
            for (Resource resource : list) {
                URL url = resource.getURL();

                Long timestamp = getTimestampFromSqlFilename(basename(url));
                if (timestamp != null) {
                    sqlmap.put(timestamp, resource);
                }

            }
        } catch (IOException e) {
            throw new SetupException("Error while scanning for SQL files: " + e.getLocalizedMessage(), e);
        }
        return sqlmap;
    }

    private Resource[] listResources(String databaseVendor) throws IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                getClass().getClassLoader());
        Resource[] resources = resolver
                .getResources("classpath:com/intel/mtwilson/attestation-hub/database/" + databaseVendor + "/*.sql");
        return resources;
    }

    Pattern pTimestampName = Pattern.compile("^([0-9]+).*");

    /**
     * Given a URL, returns the final component filename
     * <p>
     * Example URL:
     * jar:file:/C:/Users/jbuhacof/workspace/mountwilson-0.5.4/desktop/setup-
     * console/target/setup-console-0.5.4-SNAPSHOT-with-dependencies.jar!/com/
     * intel/mtwilson/database/mysql/20121226000000_remove_created_by_patch_rc3.
     * sql Example output: 20121226000000_remove_created_by_patch_rc3.sql
     *
     * @param url
     * @return
     */
    private String basename(URL url) {
        String[] parts = StringUtils.split(url.toExternalForm(), "/");
        return parts[parts.length - 1];
    }

    /**
     * @param filename without any path like:
     *                 20121226000000_remove_created_by_patch_rc3.sql
     * @return
     */
    private Long getTimestampFromSqlFilename(String filename) {
        Matcher mTimestampName = pTimestampName.matcher(filename);
        if (mTimestampName.matches()) {
            String timestamp = mTimestampName.group(1);
            return Long.valueOf(timestamp);
        }
        return null;
    }

    public boolean checkIfTableExist(String tableName) throws SQLException, ClassNotFoundException {
        log.debug("ckeckIfTableExist for tableName::" + tableName);
        List<String> tableNamesList = getTableNames(getConnection());
        boolean status = false;
        if (tableNamesList.contains(tableName)) {
            status = true;
        }
        closeConnection();
        return status;
    }

    private List<String> getTableNames(Connection c) throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        try (Statement s = c.createStatement()) {
            String sqlStmt = "";
            switch (databaseVendor) {
                case "mysql":
                    sqlStmt = "SHOW TABLES";
                    break;
                case "postgresql":
                    sqlStmt = "SELECT table_name FROM information_schema.tables ORDER BY table_name;";
                    break;
            }
            try (ResultSet rs = s.executeQuery(sqlStmt)) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        }
        /// log.info("Table Names list :"+list);
        return list;
    }

    private List<ChangelogEntry> getChangelog(Connection c) throws SQLException {
        ArrayList<ChangelogEntry> list = new ArrayList<>();
        log.debug("Listing tables...");
        // first determine if we have the new changelog table `mw_changelog`, or
        // the old one `changelog`, or none at all
        List<String> tableNames = getTableNames(c);
        boolean hasMwChangelog = false;
        boolean hasChangelog = false;
        if (tableNames.contains("mw_changelog")) {
            hasMwChangelog = true;
        }
        if (tableNames.contains("changelog")) {
            hasChangelog = true;
        }

        if (!hasChangelog && !hasMwChangelog) {
            return list; /*
             * empty list indicates database is not initialized and
             * all scripts need to be executed
             */
        }

        String changelogTableName = null;
        // if we have both changelog tables, copy all records from old changelog
        // to new changelog and then use that
        if (hasChangelog && hasMwChangelog) {
            try (PreparedStatement check = c.prepareStatement("SELECT APPLIED_AT FROM mw_changelog WHERE ID=?")) {
                try (PreparedStatement insert = c
                        .prepareStatement("INSERT INTO mw_changelog SET ID=?, APPLIED_AT=?, DESCRIPTION=?")) {
                    try (Statement select = c.createStatement()) {
                        try (ResultSet rs = select.executeQuery("SELECT ID,APPLIED_AT,DESCRIPTION FROM changelog")) {
                            while (rs.next()) {
                                check.setLong(1, rs.getLong("ID"));
                                try (ResultSet rsCheck = check.executeQuery()) {
                                    if (rsCheck.next()) {
                                        // the id is already in the new
                                        // mw_changelog table
                                    } else {
                                        insert.setLong(1, rs.getLong("ID"));
                                        insert.setString(2, rs.getString("APPLIED_AT"));
                                        insert.setString(3, rs.getString("DESCRIPTION"));
                                        insert.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            changelogTableName = "mw_changelog";
        } else if (hasMwChangelog) {
            changelogTableName = "mw_changelog";
        } else if (hasChangelog) {
            changelogTableName = "changelog";
        }
        try (Statement s = c.createStatement()) {
            try (ResultSet rs = s
                    .executeQuery(String.format("SELECT ID,APPLIED_AT,DESCRIPTION FROM %s", changelogTableName))) {
                while (rs.next()) {
                    ChangelogEntry entry = new ChangelogEntry();
                    entry.id = rs.getString("ID");
                    entry.applied_at = rs.getString("APPLIED_AT");
                    entry.description = rs.getString("DESCRIPTION");
                    list.add(entry);
                }
            }
        }
        return list;
    }

    public static Connection getConnection() throws ClassNotFoundException, SQLException {
        log.debug("Inside HubDbConnect get Connection. Url={}, user={}, driver={}", databaseUrl, databaseUsername,
                databaseDriver);
        Properties info = new Properties();
        info.put("user", databaseUsername);
        info.put("password", databasePassword);

        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        Class.forName(databaseDriver);
        connection = DriverManager.getConnection(databaseUrl, info);
        log.debug("Created DB connection " + connection);
        return connection;
    }

    private static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Errow while closing connection in DB patches", e);
            }
        }
    }
}
