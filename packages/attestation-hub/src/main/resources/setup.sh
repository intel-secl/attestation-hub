#!/bin/bash

# Attestation Hub install script
# Outline:
# 1. source the "functions.sh" file:  mtwilson-linux-util-3.0-SNAPSHOT.sh
# 2. load existing environment configuration
# 3. look for ~/attestation-hub.env and source it if it's there
# 4. determine if we are installing as root or non-root user; set paths
# 5. prompt for installation variables if they are not provided
# 6. detect java, if java not installed, and we have it bundled, install it
# 7. detect postgres, install if not installed
# 8. unzip attestation-hub archive attestation-hub-zip-0.1-SNAPSHOT.zip into /opt/attestation-hub, overwrite if any files already exist
# 9. link /usr/local/bin/attestation-hub -> /opt/attestation-hub/bin/attestation-hub, if not already there
# 10. add attestation-hub to startup services
# 11. look for ATTESTATION_HUB_PASSWORD environment variable; if not present print help message and exit:
#     Attestation Hub requires a master password
#     losing the master password will result in data loss
# 12. attestation-hub setup
# 13. attestation-hub start

#####

# default settings
# note the layout setting is used only by this script
# and it is not saved or used by the app script
export ATTESTATION_HUB_HOME=${ATTESTATION_HUB_HOME:-/opt/attestation-hub}
ATTESTATION_HUB_LAYOUT=${ATTESTATION_HUB_LAYOUT:-home}

# LOG DEFAULT SETTING
export LOG_ROTATION_PERIOD=${LOG_ROTATION_PERIOD:-weekly}
export LOG_COMPRESS=${LOG_COMPRESS:-compress}
export LOG_DELAYCOMPRESS=${LOG_DELAYCOMPRESS:-delaycompress}
export LOG_COPYTRUNCATE=${LOG_COPYTRUNCATE:-copytruncate}
export LOG_SIZE=${LOG_SIZE:-1G}
export LOG_OLD=${LOG_OLD:-12}
export ATTESTATION_HUB_PORT_HTTP=${ATTESTATION_HUB_PORT_HTTP:-19082}
export ATTESTATION_HUB_PORT_HTTPS=${ATTESTATION_HUB_PORT_HTTPS:-19445}
export ATTESTATION_HUB_POLL_INTERVAL=${ATTESTATION_HUB_POLL_INTERVAL:-2}
export ATTESTATION_HUB_DB_USERNAME=${ATTESTATION_HUB_DB_USERNAME:-root}
export ATTESTATION_HUB_DB_NAME=${ATTESTATION_HUB_DB_NAME:-attestation_hub_pu}
export ATTESTATION_HUB_DB_HOSTNAME=${ATTESTATION_HUB_DB_HOSTNAME:-localhost}
export ATTESTATION_HUB_DB_PORTNUM=${ATTESTATION_HUB_DB_PORTNUM:-5432}
export ATTESTATION_HUB_DB_SSLMODE=${ATTESTATION_HUB_DB_SSLMODE:-require}
export ATTESTATION_HUB_DB_DRIVER=${ATTESTATION_HUB_DB_DRIVER:-org.postgresql.Driver}
export MTWILSON_SERVER_PORT=${MTWILSON_SERVER_PORT:-8443}
export ATTESTATION_HUB_NOSETUP=${ATTESTATION_HUB_NOSETUP:-false}
export POSTGRESQL_KEEP_PGPASS=${POSTGRESQL_KEEP_PGPASS:-true}

# the env directory is not configurable; it is defined as ATTESTATION_HUB_HOME/env and
# the administrator may use a symlink if necessary to place it anywhere else
export ATTESTATION_HUB_ENV=$ATTESTATION_HUB_HOME/env

# 1. source the "functions.sh" file:  mtwilson-linux-util-3.0-SNAPSHOT.sh
# functions script (mtwilson-linux-util-3.0-SNAPSHOT.sh) is required
# we use the following functions:
# java_detect java_ready_report 
# echo_failure echo_warning
# register_startup_script
UTIL_SCRIPT_FILE=$(ls -1 mtwilson-linux-util-*.sh | head -n 1)
if [ -n "$UTIL_SCRIPT_FILE" ] && [ -f "$UTIL_SCRIPT_FILE" ]; then
  . $UTIL_SCRIPT_FILE
fi

ATTESTATION_HUB_UTIL_SCRIPT_FILE=$(ls -1 attestation-hub-functions.sh | head -n 1)
if [ -n "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ] && [ -f "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ]; then
  . $ATTESTATION_HUB_UTIL_SCRIPT_FILE
fi

if [ "$ATTESTATION_HUB_DB_SSLMODE" == "verify-ca" ] || [ "$ATTESTATION_HUB_DB_SSLMODE" == "verify-full" ]; then
    if [ -z "$ATTESTATION_HUB_DB_SSLROOTCERT" ]; then
        echo_failure "Database server certificate file not specified"
        exit 1
    else
        if [ ! -f "$ATTESTATION_HUB_DB_SSLROOTCERT" ]; then
            echo_failure "Database server certificate file does not exist"
            exit 1
        fi
    fi
fi

# 2. load existing environment configuration if already defined
EXTENSIONS_CACHE_FILE=$ATTESTATION_HUB_HOME/configuration/extensions.cache
echo "EXTENSIONS_CACHE_FILE:: $EXTENSIONS_CACHE_FILE"
if [ -f $EXTENSIONS_CACHE_FILE ] ; then
echo "removing existing extension cache file"
    rm -rf $EXTENSIONS_CACHE_FILE
fi

if [ -d $ATTESTATION_HUB_ENV ]; then
  ATTESTATION_HUB_ENV_FILES=$(ls -1 $ATTESTATION_HUB_ENV/*)
  for env_file in $ATTESTATION_HUB_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# 3. look for ~/attestation-hub.env and source it if it's there
if [ -f ~/attestation-hub.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/attestation-hub.env"
  . ~/attestation-hub.env
  env_file_exports=$(cat ~/attestation-hub.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi

# 4. determine if we are installing as root or non-root user; set paths
if [ "$(whoami)" == "root" ]; then
  # create a attestation-hub user if there isn't already one created
  ATTESTATION_HUB_USERNAME=${ATTESTATION_HUB_USERNAME:-attestation-hub}
  if ! getent passwd $ATTESTATION_HUB_USERNAME 2>&1 >/dev/null; then
    useradd --comment "Mt Wilson Attestation Hub" --home $ATTESTATION_HUB_HOME --system --shell /bin/false $ATTESTATION_HUB_USERNAME
    usermod --lock $ATTESTATION_HUB_USERNAME
    # note: to assign a shell and allow login you can run "usermod --shell /bin/bash --unlock $ATTESTATION_HUB_USERNAME"
  fi
else
  # already running as attestation-hub user
  ATTESTATION_HUB_USERNAME=$(whoami)
  echo_warning "Running as $ATTESTATION_HUB_USERNAME; if installation fails try again as root"
  if [ ! -w "$ATTESTATION_HUB_HOME" ] && [ ! -w $(dirname $ATTESTATION_HUB_HOME) ]; then
    export ATTESTATION_HUB_HOME=$(cd ~ && pwd)
  fi
fi

# if an existing attestation-hub is already running, stop it while we install
if which attestation-hub; then
  attestation-hub stop
fi

# define application directory layout
if [ "$ATTESTATION_HUB_LAYOUT" == "linux" ]; then
  export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-/etc/attestation-hub}
  export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-/var/opt/attestation-hub}
  export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-/var/log/attestation-hub}
elif [ "$ATTESTATION_HUB_LAYOUT" == "home" ]; then
  export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-$ATTESTATION_HUB_HOME/configuration}
  export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-$ATTESTATION_HUB_HOME/repository}
  export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-$ATTESTATION_HUB_HOME/logs}
fi
export ATTESTATION_HUB_BIN=$ATTESTATION_HUB_HOME/bin
export ATTESTATION_HUB_JAVA=$ATTESTATION_HUB_HOME/java

# note that the env dir is not configurable; it is defined as "env" under home
export ATTESTATION_HUB_ENV=$ATTESTATION_HUB_HOME/env
export POSTGRES_LOG="$ATTESTATION_HUB_LOGS/intel.postgres.err"

attestation_hub_backup_configuration() {
  if [ -n "$ATTESTATION_HUB_CONFIGURATION" ] && [ -d "$ATTESTATION_HUB_CONFIGURATION" ]; then
    datestr=`date +%Y%m%d.%H%M`
	mkdir -p /var/backup/
    backupdir=/var/backup/attestation-hub.configuration.$datestr
    cp -r $ATTESTATION_HUB_CONFIGURATION $backupdir
  fi
}

attestation_hub_backup_repository() {
  if [ -n "$ATTESTATION_HUB_REPOSITORY" ] && [ -d "$ATTESTATION_HUB_REPOSITORY" ]; then
	mkdir -p /var/backup/
    datestr=`date +%Y%m%d.%H%M`
    backupdir=/var/backup/attestation-hub.repository.$datestr
    cp -r $ATTESTATION_HUB_REPOSITORY $backupdir
  fi
}

# backup current configuration and data, if they exist
attestation_hub_backup_configuration
attestation_hub_backup_repository

if [ -d $ATTESTATION_HUB_CONFIGURATION ]; then
  backup_conf_dir=$ATTESTATION_HUB_REPOSITORY/backup/configuration.$(date +"%Y%m%d.%H%M")
  mkdir -p $backup_conf_dir
  cp -R $ATTESTATION_HUB_CONFIGURATION/* $backup_conf_dir
fi

# create application directories (chown will be repeated near end of this script, after setup)
for directory in $ATTESTATION_HUB_HOME $ATTESTATION_HUB_CONFIGURATION $ATTESTATION_HUB_ENV $ATTESTATION_HUB_REPOSITORY $ATTESTATION_HUB_LOGS; do
  mkdir -p $directory
  chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $directory
  chmod 700 $directory
done

ATTESTATION_HUB_DB_SERVERCERT=$ATTESTATION_HUB_CONFIGURATION/ahubdbcert.crt
cp "$ATTESTATION_HUB_DB_SSLROOTCERT" "$ATTESTATION_HUB_DB_SERVERCERT"

# store directory layout in env file
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_HOME=$ATTESTATION_HUB_HOME" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_CONFIGURATION=$ATTESTATION_HUB_CONFIGURATION" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_REPOSITORY=$ATTESTATION_HUB_REPOSITORY" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_JAVA=$ATTESTATION_HUB_JAVA" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_BIN=$ATTESTATION_HUB_BIN" >> $ATTESTATION_HUB_ENV/attestation-hub-layout
echo "export ATTESTATION_HUB_LOGS=$ATTESTATION_HUB_LOGS" >> $ATTESTATION_HUB_ENV/attestation-hub-layout

# store attestation-hub username in env file
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-username
echo "export ATTESTATION_HUB_USERNAME=$ATTESTATION_HUB_USERNAME" >> $ATTESTATION_HUB_ENV/attestation-hub-username

# store the auto-exported environment variables in env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name=$env_file_var_value" >> $ATTESTATION_HUB_ENV/attestation-hub-setup
done

# if properties file exists
ATTESTATION_HUB_PROPERTIES_FILE=${ATTESTATION_HUB_PROPERTIES_FILE:-"$ATTESTATION_HUB_CONFIGURATION/attestation-hub.properties"}

if [ -e $ATTESTATION_HUB_PROPERTIES_FILE ]; then
	attestation-hub export-config --in=/opt/attestation-hub/configuration/attestation-hub.properties --out=/opt/attestation-hub/configuration/attestation-hub.properties
fi

touch "$ATTESTATION_HUB_PROPERTIES_FILE"
chown "$ATTESTATION_HUB_USERNAME":"$ATTESTATION_HUB_USERNAME" "$ATTESTATION_HUB_PROPERTIES_FILE"
chmod 600 "$ATTESTATION_HUB_PROPERTIES_FILE"

# load existing environment; set variables will take precendence
load_attestation_hub_conf
load_attestation_hub_defaults

#------------------- promts starts
# 4. prompt for installation variables if they are not provided
export ATTESTATION_HUB_DB_PASSWORD=${ATTESTATION_HUB_DB_PASSWORD:-$(generate_password 16)} 
#required database properties
prompt_with_default ATTESTATION_HUB_DB_NAME "Attestation-Hub db name:" "$ATTESTATION_HUB_DB_NAME"
update_property_in_file "attestation-hub.db.name" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_NAME"

prompt_with_default ATTESTATION_HUB_DB_HOSTNAME "Attestation-Hub db Hostname:" "$ATTESTATION_HUB_DB_HOSTNAME"
update_property_in_file "attestation-hub.db.host" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_HOSTNAME"

prompt_with_default ATTESTATION_HUB_DB_PORTNUM "Attestation-Hub db Portno:" "$ATTESTATION_HUB_DB_PORTNUM"
update_property_in_file "attestation-hub.db.port" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_PORTNUM"

prompt_with_default ATTESTATION_HUB_DB_SSLMODE "Attestation-Hub db SSLMode:" "$ATTESTATION_HUB_DB_SSLMODE"
update_property_in_file "attestation-hub.db.sslmode" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_SSLMODE"

if [ -f "$ATTESTATION_HUB_DB_SERVERCERT" ]; then
    export POSTGRES_SSLROOTCERT=${ATTESTATION_HUB_DB_SERVERCERT}
    update_property_in_file "attestation-hub.db.sslrootcert" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_SERVERCERT"
fi

update_property_in_file "attestation-hub.db.user" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_USERNAME"

update_property_in_file "attestation-hub.db.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_PASSWORD"

prompt_with_default ATTESTATION_HUB_DB_DRIVER "Attestation-Hub db driver:" "$ATTESTATION_HUB_DB_DRIVER"
update_property_in_file "attestation-hub.db.driver" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_DRIVER"

export ATTESTATION_HUB_DB_URL="jdbc:postgresql://${ATTESTATION_HUB_DB_HOSTNAME}:${ATTESTATION_HUB_DB_PORTNUM}/${ATTESTATION_HUB_DB_NAME}"
update_property_in_file "attestation-hub.db.url" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_URL"

export POSTGRES_HOSTNAME=${ATTESTATION_HUB_DB_HOSTNAME}
export POSTGRES_PORTNUM=${ATTESTATION_HUB_DB_PORTNUM}
export POSTGRES_DATABASE=${ATTESTATION_HUB_DB_NAME}
export POSTGRES_USERNAME=${ATTESTATION_HUB_DB_USERNAME}
export POSTGRES_PASSWORD=${ATTESTATION_HUB_DB_PASSWORD}
export POSTGRES_SSLMODE=${ATTESTATION_HUB_DB_SSLMODE}

# modifying after mtwilson api client built
prompt_with_default MTWILSON_SERVER "Mtwilson Server:" "$MTWILSON_SERVER"
update_property_in_file "mtwilson.server" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER"
prompt_with_default MTWILSON_SERVER_PORT "Mtwilson Server Port:" "$MTWILSON_SERVER_PORT"
update_property_in_file "mtwilson.server.port" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER_PORT"

export MTWILSON_API_URL=https://${MTWILSON_SERVER}:${MTWILSON_SERVER_PORT}/mtwilson/v2
prompt_with_default MTWILSON_API_URL "Mtwilson API Url:" "$MTWILSON_API_URL"
update_property_in_file "mtwilson.api.url" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_API_URL"

prompt_with_default ATTESTATION_HUB_POLL_INTERVAL "Attestation Hub Poll Interval:" "$ATTESTATION_HUB_POLL_INTERVAL"
update_property_in_file "attestation-hub.poll.interval" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_POLL_INTERVAL"

update_property_in_file "attestation-hub.saml.timeout" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_SAML_TIMEOUT"

# Jetty TLS configuration

if [ -n "$AH_TLS_CERT_IP" ]; then
    update_property_in_file "jetty.tls.cert.ip" "$ATTESTATION_HUB_PROPERTIES_FILE" "$AH_TLS_CERT_IP"
fi

if [ -n "$AH_TLS_CERT_DNS" ]; then
    update_property_in_file "jetty.tls.cert.dns" "$ATTESTATION_HUB_PROPERTIES_FILE" "$AH_TLS_CERT_DNS"
fi


#------------------- promts end

#------------------- packages installation starts
# 6. detect java, if java not installed, and we have it bundled, install it
# attestation-hub requires java 1.8 or later
if [ "$IS_RPM" != "true" ]; then

    echo "Installing Java..."
    JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.8}
    java_install_openjdk
    JAVA_CMD=$(type -p java | xargs readlink -f)
    JAVA_HOME=$(dirname $JAVA_CMD | xargs dirname | xargs dirname)
    JAVA_REQUIRED_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
    echo "# $(date)" > $ATTESTATION_HUB_ENV/attestation-hub-java
    echo "export JAVA_HOME=$JAVA_HOME" >> $ATTESTATION_HUB_ENV/attestation-hub-java
    echo "export JAVA_CMD=$JAVA_CMD" >> $ATTESTATION_HUB_ENV/attestation-hub-java
    echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> $ATTESTATION_HUB_ENV/attestation-hub-java

    if [ -f "${JAVA_HOME}/jre/lib/security/java.security" ]; then
      echo "Replacing java.security file, existing file will be backed up"
      backup_file "${JAVA_HOME}/jre/lib/security/java.security"
      cp java.security "${JAVA_HOME}/jre/lib/security/java.security"
    fi
fi
# libguestfs packages has a custom prompt about installing supermin which ignores the �-y� option we provide to apt-get. Following code will help to avoid that prompt 
#export DEBIAN_FRONTEND=noninteractive
#echo libguestfs-tools libguestfs/update-appliance boolean true | debconf-set-selections

# make sure unzip, openssl is installed
if [ "$IS_RPM" != "true" ]; then
	ATTESTATION_HUB_YUM_PACKAGES="unzip openssl"
fi
ATTESTATION_HUB_APT_PACKAGES="unzip"
ATTESTATION_HUB_YAST_PACKAGES="unzip"
ATTESTATION_HUB_ZYPPER_PACKAGES="unzip"
install_packages "Installer requirements" "ATTESTATION_HUB"
if [ $? -ne 0 ]; then echo_failure "Failed to install prerequisites through package installer"; exit -1; fi

# 7. detect postgres, install if not installed
postgres_write_connection_properties "$ATTESTATION_HUB_CONFIGURATION/attestation-hub.properties" attestation-hub.db
 
postgres_installed=1
touch ${ATTESTATION_HUB_HOME}/.pgpass
chmod 0600 ${ATTESTATION_HUB_HOME}/.pgpass
chown ${ATTESTATION_HUB_USERNAME}:${ATTESTATION_HUB_USERNAME} ${ATTESTATION_HUB_HOME}/.pgpass
export POSTGRES_HOSTNAME POSTGRES_PORTNUM POSTGRES_DATABASE POSTGRES_USERNAME POSTGRES_PASSWORD POSTGRES_SSLMODE POSTGRES_SSLROOTCERT
if [ "$POSTGRES_HOSTNAME" == "127.0.0.1" ] || [ "$POSTGRES_HOSTNAME" == "localhost" ]; then
  PGPASS_HOSTNAME=localhost
else
  PGPASS_HOSTNAME="$POSTGRES_HOSTNAME"
fi
echo "$POSTGRES_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD:$POSTGRES_SSLMODE:$POSTGRES_SSLROOTCERT" > ${ATTESTATION_HUB_HOME}/.pgpass
echo "$PGPASS_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD:$POSTGRES_SSLMODE:$POSTGRES_SSLROOTCERT" >> ${ATTESTATION_HUB_HOME}/.pgpass
if [ $(whoami) == "root" ]; then cp ${ATTESTATION_HUB_HOME}/.pgpass ~/.pgpass; fi


ATTESTATION_HUB_PORT_HTTP=${ATTESTATION_HUB_PORT_HTTP:-${JETTY_PORT:-80}}
ATTESTATION_HUB_PORT_HTTPS=${ATTESTATION_HUB_PORT_HTTPS:-${JETTY_SECURE_PORT:-443}}

# delete existing java files, to prevent a situation where the installer copies
# a newer file but the older file is also there
if [ -d $ATTESTATION_HUB_HOME/java ]; then
  rm $ATTESTATION_HUB_HOME/java/*.jar
fi
#------------------- packages installation starts

# 8. unzip attestation-hub archive attestation-hub-zip-0.1-SNAPSHOT.zip into /opt/attestation-hub, overwrite if any files already exist
echo "Extracting application..."
ATTESTATION_HUB_ZIPFILE=`ls -1 attestation-hub-*.zip 2>/dev/null | head -n 1`
unzip -oq $ATTESTATION_HUB_ZIPFILE -d $ATTESTATION_HUB_HOME

# copy utilities script file to application folder
cp $UTIL_SCRIPT_FILE $ATTESTATION_HUB_HOME/bin/functions.sh

# set permissions
chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $ATTESTATION_HUB_HOME
chmod 755 $ATTESTATION_HUB_HOME/bin/*

# 9. link /usr/local/bin/attestation-hub -> /opt/attestation-hub/bin/attestation-hub, if not already there
EXISTING_ATTESTATION_HUB_COMMAND=`which attestation-hub`
if [ -z "$EXISTING_ATTESTATION_HUB_COMMAND" ]; then
  ln -s $ATTESTATION_HUB_HOME/bin/attestation-hub.sh /usr/local/bin/attestation-hub
  #ln -s $ATTESTATION_HUB_HOME/bin/attestation-hub.sh /usr/bin/attestation-hub
fi
if [[ ! -h $ATTESTATION_HUB_BIN/attestation-hub ]]; then
  ln -s $ATTESTATION_HUB_BIN/attestation-hub.sh $ATTESTATION_HUB_BIN/attestation-hub
fi

# 10. add attestation-hub to startup services
# RHEL 7.6 needs PID file for systemd startup service 
# The location is identified as below in attestation-hub.sh
ATTESTATION_HUB_PID_FILE=$ATTESTATION_HUB_HOME/attestation-hub.pid

register_startup_script $ATTESTATION_HUB_BIN/attestation-hub attestation-hub $ATTESTATION_HUB_PID_FILE

disable_tcp_timestamps

# 11. look for ATTESTATION_HUB_PASSWORD environment variable; if not present print help message and exit:
#     Attestation Hub requires a master password
#     losing the master password will result in data loss
# setup the attestation-hub, unless the NOSETUP variable is set to true
if [ "$ATTESTATION_HUB_NOSETUP" = "false" ]; then
  attestation-hub setup
else
  echo_warning "ATTESTATION_HUB_NOSETUP variable is set. Skipping setup..."
fi

# delete the temporary setup environment variables file
rm -f $ATTESTATION_HUB_ENV/attestation-hub-setup
rm -f ~/.pgpass

# ensure the attestation-hub owns all the content created during setup
for directory in $ATTESTATION_HUB_HOME $ATTESTATION_HUB_CONFIGURATION $ATTESTATION_HUB_JAVA $ATTESTATION_HUB_BIN $ATTESTATION_HUB_ENV $ATTESTATION_HUB_REPOSITORY $ATTESTATION_HUB_LOGS; do
  chown -R $ATTESTATION_HUB_USERNAME:$ATTESTATION_HUB_USERNAME $directory
done

#attestation-hub import-config --in=/opt/attestation-hub/configuration/attestation-hub.properties --out=/opt/attestation-hub/configuration/attestation-hub.properties

# Log Rotate
mkdir -p /etc/logrotate.d
if [ ! -a /etc/logrotate.d/attestation-hub ]; then
 echo "/opt/attestation-hub/logs/attestation-hub.log {
    missingok
	notifempty
	rotate $LOG_OLD
	maxsize $LOG_SIZE
    nodateext
	$LOG_ROTATION_PERIOD 
	$LOG_COMPRESS
	$LOG_DELAYCOMPRESS
	$LOG_COPYTRUNCATE
}" > /etc/logrotate.d/attestation-hub
fi

# 13. attestation-hub start
# start the server, unless the NOSETUP variable is defined
if [ "$ATTESTATION_HUB_NOSETUP" = "false" ]; then
  attestation-hub start
else
  echo_info "Run attestation-hub setup and start server."
fi
echo_success "Installation complete"
