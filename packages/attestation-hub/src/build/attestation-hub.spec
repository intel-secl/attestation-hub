# RPM SPEC for ISecL Attestation Hub
%global attestation_hub_home %{_datadir}/%{name}
%global attestation_hub_conf %{_sysconfdir}/%{name}
%global attestation_hub_env %{attestation_hub_home}/env
%global attestation_hub_java %{_javadir}/%{name}
%global attestation_hub_log %{_localstatedir}/log/%{name}
%global attestation_hub_tenantconfig %{_sysconfdir}/tenantconfig
%global attestation_hub_username attestation-hub

%define debug_package %{nil}
%define __jar_repack %{nil}

#Attestation Hub Log Rotation Defaults
%define attestation_hub_log_rotation_period monthly
%define attestation_hub_log_compress compress
%define attestation_hub_log_delaycompress delaycompress
%define attestation_hub_log_copytruncate copytruncate
%define attestation_hub_log_size 1G
%define attestation_hub_log_old 12

#Attestation Hub RPM Metadata
Name:               attestation-hub
Version:            4.5
Release:            1%{?dist}
Summary:            Self-extracting RPM that installs the attestation-hub
Group:              Applications/System
License:            BSD
URL:                https://github.com/intel-secl/attestation-hub
Source0:            attestation-hub.tar.gz
Source1:            common-libs-hub.tar.gz
Source2:            verification-service.tar.gz
BuildRoot:          %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
BuildArch:          x86_64
ExclusiveArch:      x86_64

#Dependencies to Build Attestation Hub RPM from Sources
%if 0%{?centos}
BuildRequires: rh-maven35
BuildRequires: epel-release
%endif

%if 0%{?rhel}
BuildRequires: maven
BuildRequires: epel-release
%endif

%if 0%{?fedora}
BuildRequires: maven
%endif

BuildRequires:     wget
#BuildRequires:     git
BuildRequires:     zip
BuildRequires:     unzip
BuildRequires:     ant
BuildRequires:     gcc
BuildRequires:     patch
BuildRequires:     gcc-c++
BuildRequires:     trousers-devel
BuildRequires:     openssl-devel
#BuildRequires:     epel-release
BuildRequires:     makeself
BuildRequires:     rpm-build
BuildRequires:     deltarpm

#Attestation Hub Runtime Dependencies
Requires(pre): shadow-utils
Requires(postun): shadow-utils
Requires(post): chkconfig
Requires(preun): chkconfig
Requires(preun): initscripts
Requires(postun): initscripts
Requires:       java-1.8.0-openjdk-devel
Requires:       openssl
Requires:       openssl-devel
Requires:       logrotate
Requires:       postgresql = 11.5
Requires:       postgresql-server = 11.5
Requires:       postgresql-contrib = 11.5
Requires:       xmlstarlet
#Requires:       policycoreutils
Requires:       unzip
Requires:       zip
#Requires:       wget
#Requires:       net-tools
#Requires:       policycoreutils-python

# Java Component Dependencies not Packaged with Attestation Hub
Requires:       apache-commons-beanutils = 1.9.3
Requires:       apache-commons-codec = 1.11
Requires:       apache-commons-collections = 3.2.2
Requires:       apache-commons-configuration = 1.10
Requires:       apache-commons-digester = 2.1
Requires:       apache-commons-exec = 1.3
Requires:       apache-commons-lang = 2.6
Requires:       apache-commons-lang3 = 3.7
Requires:       google-gson = 2.8.2
#Requires:       jetty-http
#Requires:       jetty-io
#Requires:       jetty-security
#Requires:       jetty-server
#Requires:       jetty-servlet
#Requires:       jetty-util
#Requires:       jetty-webapp
#Requires:       jetty-xml
Requires:       joda-time = 2.9.9
Requires:       velocity = 1.7
#Requires:       jetty-continuation = 9.4.14
Requires:       objenesis = 2.6
Requires:       antlr = 2.7.7
#Requires:       opensaml-java-core
#Requires:       opensaml-java-messaging-api
#Requires:       opensaml-java-profile-api
#Requires:       opensaml-java-saml-api
#Requires:       opensaml-java-saml-impl
#Requires:       opensaml-java-security-api
#Requires:       opensaml-java-security-impl
#Requires:       opensaml-java-soap-api
#Requires:       opensaml-java-soap-impl
#Requires:       opensaml-java-storage-api
#Requires:       opensaml-java-xmlsec-api
#Requires:       opensaml-java-xmlsec-impl

Requires:       apache-commons-compress
Requires:       apache-commons-io
Requires:       apache-commons-logging
Requires:       apache-commons-validator
Requires:       cryptacular
Requires:       jackson-annotations
Requires:       jackson-core
Requires:       jackson-databind
Requires:       jackson-dataformat-xml
Requires:       jackson-dataformat-yaml
Requires:       jackson-jaxrs-json-provider
Requires:       jackson-jaxrs-xml-provider
Requires:       jackson-module-jaxb-annotations
#Requires:       jdbi
Requires:       slf4j = 1.7.25	
Requires:       snakeyaml
Requires:       stax2-api
#Requires:       woodstox-core = 5.0.3
Requires:       xstream
Requires:       xpp3-minimal
#Requires:       jetty-continuation = 9.4.14
Requires:       objenesis = 2.6
Requires:       glassfish-hk2-api
Requires:       glassfish-hk2-locator
Requires:       glassfish-hk2-utils
Requires:       glassfish-servlet-api
#Requires:       jersey
Requires:       mimepull = 1.9.6

#Attestation Hub Provides
Provides:       %{name} = %{version}-%{release}
Provides:       mtwilson-configuration == 4.5
Provides:       attestation-hub-api
Provides:       attestation-cache-hub-client-jaxrs2
Provides:       attestation-hub-common
Provides:       attestation-hub-postgresql
Provides:       attestation-hub-setup
Provides:       attestation-hub-version
Provides:       attestation-hub-core
Provides:       attestation-hub-persistence
Provides:       attestation-hub-plugins
Provides:       attestation-hub-plugins-api
Provides:       attestation-hub-kubernetes-plugin
Provides:       attestation-hub-mesos-plugin
Provides:       attestation-hub-nova-plugin
Provides:       attestation-hub-ws
Provides:       host-verification-service-client-java8
Provides:       mtwilson-configuration-settings-ws-v2
Provides:       mtwilson-core-html5-login-token
Provides:       mtwilson-core-feature-inventory
Provides:       mtwilson-core-jetty9
Provides:       mtwilson-core-login-token
Provides:       mtwilson-core-setup
Provides:       mtwilson-extensions-cache
Provides:       mtwilson-extensions-ws-v2
Provides:       mtwilson-flavor-client-jaxrs2
Provides:       mtwilson-http-servlets
Provides:       mtwilson-maven-java
Provides:       mtwilson-util-jaxrs2-client
Provides:       mtwilson-util-io
Provides:       mtwilson-util-configuration
Provides:       mtwilson-util-exec
Provides:       mtwilson-util-tls-policy
Provides:       mtwilson-util-console
Provides:       mtwilson-util-crypto
Provides:       mtwilson-util-extensions
Provides:       mtwilson-util-xml
Provides:       mtwilson-util-jersey2
Provides:       mtwilson-user-management-client-jaxrs2
Provides:       mtwilson-password-vault
Provides:       mtwilson-setup-ext
Provides:       mtwilson-shiro-util
Provides:       mtwilson-shiro-file
Provides:       mtwilson-privacyca-client-jaxrs2
Provides:       mtwilson-launcher-api
Provides:       mtwilson-launcher
Provides:       mtwilson-version-ws-v2
Provides:       mtwilson-linux-util
Provides:       lib-verifier

%description
The Attestation Hub retrieves the attestation report from Verification Service at a configurable interval, and after verification pushes them to the various backend systems configured

%prep
%setup -q -n attestation-hub
%setup -q -T -D -b 1 -n .
%setup -q -T -D -b 2 -n .

%build
#TODO: Build from Attestation hub Sources
declare -a AH_REPOSITORY_ORDER
AH_REPOSITORY_ORDER=(
external-artifacts
contrib
common-java
lib-common
lib-privacyca
lib-tpm-provider
lib-platform-info
lib-host-connector
lib-flavor
lib-verifier
lib-saml
lib-asset-tag-provisioner
lib-asset-tag-creator
privacyca
verification-service
attestation-hub
)

which mvn
if [ $? -ne 0 ]; then
   echo "ERROR: MAVEN not installed or set in PATH"
   exit 1;
fi


ant_build_repos() {
  local start_repo=${2}
  local required_to_build=false
  cat /dev/null > /tmp/ant.log
  echo "Running ant build on repositories (log file: ant.log)..."
  for repo in ${!AH_REPOSITORY_ORDER[@]}; do
    local repo_name=${AH_REPOSITORY_ORDER[${repo}]}
    if [ -n "${start_repo}" ] && ! $required_to_build && [ "${repo_name}" != "${start_repo}" ]; then
      echo "Skipping ant build in repository [${repo_name}]..."
      continue
    else
      required_to_build=true
    fi
    echo "Running ant build for repository [${repo_name}]..."
    (
    cd "${repo_name}"
    ant > /tmp/ant.log 2>&1
    )
    local return_code=$?
    if [ ${return_code} -ne 0 ]; then
      echo "ERROR: Issue while running build on repository [${repo_name}]"
      return ${return_code}
    fi
  done
}

ant_build_repos
if [ $? -ne 0 ]; then exit 10; fi

cp -r %{_topdir}/BUILD/attestation-hub/packages/attestation-hub/target/attestation-hub-%{version}-SNAPSHOT-rpm.tar.gz %{_topdir}/BUILD/.

rm -rf ${AH_REPOSITORY_ORDER[@]}

tar -xf attestation-hub-%{version}-SNAPSHOT-rpm.tar.gz

%install

rm -rf %{buildroot}
mkdir -p %{buildroot}/%{_sbindir}
mkdir -p %{buildroot}/%{_sysconfdir}/%{name}
mkdir -p %{buildroot}/%{_sysconfdir}/logrotate.d
mkdir -p %{buildroot}/%{attestation_hub_home}
mkdir -p %{buildroot}/%{attestation_hub_conf}
mkdir -p %{buildroot}/%{attestation_hub_env}
mkdir -p %{buildroot}/%{_javadir}/%{name}
mkdir -p %{buildroot}/%{attestation_hub_home}/features
mkdir -p %{buildroot}/%{attestation_hub_home}/backup
mkdir -p %{buildroot}/%{attestation_hub_home}/scripts
mkdir -p %{buildroot}/%{attestation_hub_log}
mkdir -p %{buildroot}/%{attestation_hub_tenantconfig}
mkdir -p %{buildroot}/%{attestation_hub_home}/repository

#Verification Service Essential Files
%define app_target attestation-hub-%{version}-SNAPSHOT
unzip -o attestation-hub-%{version}-SNAPSHOT/attestation-hub-zip-%{version}-SNAPSHOT-application.zip

cp bin/*                                             %{buildroot}/%{_sbindir} 
cp configuration/*                                   %{buildroot}/%{attestation_hub_conf}
cp -r features/*                                     %{buildroot}/%{attestation_hub_home}/features
cp java/*                                            %{buildroot}/%{_javadir}/%{name}
cp %{app_target}/java.security                       %{buildroot}/%{attestation_hub_home}/backup
cp %{app_target}/attestation-hub-functions.sh        %{buildroot}/%{_sbindir}/attestation-hub-functions.sh
cp %{app_target}/mtwilson-linux-util-4.5-SNAPSHOT.sh %{buildroot}/%{attestation_hub_home}/scripts/functions.sh
cp %{app_target}/attestation-hub-functions.sh        %{buildroot}/%{attestation_hub_home}/scripts/attestation-hub-functions.sh

touch %{buildroot}/%{attestation_hub_log}/attestation-hub.log

%pre
# load existing environment configuration if already defined
EXTENSIONS_CACHE_FILE=%{attestation_hub_conf}/extensions.cache
echo "EXTENSIONS_CACHE_FILE:: $EXTENSIONS_CACHE_FILE"
if [ -f $EXTENSIONS_CACHE_FILE ] ; then
echo "removing existing extension cache file"
    rm -rf $EXTENSIONS_CACHE_FILE
fi

if [ -d %{attestation_hub_env} ]; then
  ATTESTATION_HUB_ENV_FILES=$(ls -1 %{attestation_hub_env}/*)
  for env_file in $ATTESTATION_HUB_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# look for ~/attestation-hub.env and source it if it's there
if [ -f ~/attestation-hub.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/attestation-hub.env"
  . ~/attestation-hub.env
  env_file_exports=$(cat ~/attestation-hub.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file. Setup attestation-hub.env to install the RPM"
  exit 1
fi


# define application directory layout
export ATTESTATION_HUB_CONFIGURATION=${ATTESTATION_HUB_CONFIGURATION:-%{attestation_hub_conf}}
export ATTESTATION_HUB_REPOSITORY=${ATTESTATION_HUB_REPOSITORY:-%{attestation_hub_home}/repository}
export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-%{attestation_hub_log}}
export ATTESTATION_HUB_BIN=%{buildroot}/%{_sbindir} 
export ATTESTATION_HUB_JAVA=%{attestation_hub_java}

# note that the env dir is not configurable; it is defined as "env" under home
export ATTESTATION_HUB_ENV=%{attestation_hub_env}
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

# Create AH User
if ! getent passwd %{attestation_hub_username} >/dev/null; then \
    useradd -r -s /sbin/nologin \
    -c "Mt Wilson Attestation Hub" %{attestation_hub_username}
    usermod --lock %{attestation_hub_username}
fi

%post
 
# look for ~/attestation-hub.env and source it if it's there
if [ -f ~/attestation-hub.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/attestation-hub.env"
  . ~/attestation-hub.env
  env_file_exports=$(cat ~/attestation-hub.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi

ATTESTATION_HUB_INSTALL_LOG_FILE=${ATTESTATION_HUB_INSTALL_LOG_FILE:-"$ATTESTATION_HUB_LOGS/attestation-hub_install.log"}
export INSTALL_LOG_FILE="$ATTESTATION_HUB_INSTALL_LOG_FILE"
touch "$ATTESTATION_HUB_INSTALL_LOG_FILE"
chown "%{attestation_hub_username}":"%{attestation_hub_username}" "$ATTESTATION_HUB_INSTALL_LOG_FILE"
chmod 600 "$ATTESTATION_HUB_INSTALL_LOG_FILE"

echo "install log file is" $INSTALL_LOG_FILE

# source the "functions.sh"
UTIL_SCRIPT_FILE=%{attestation_hub_home}/scripts/functions.sh 
if [ -n "$UTIL_SCRIPT_FILE" ] && [ -f "$UTIL_SCRIPT_FILE" ]; then
  . $UTIL_SCRIPT_FILE
fi

ATTESTATION_HUB_UTIL_SCRIPT_FILE=%{attestation_hub_home}/scripts/attestation-hub-functions.sh
if [ -n "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ] && [ -f "$ATTESTATION_HUB_UTIL_SCRIPT_FILE" ]; then
  . $ATTESTATION_HUB_UTIL_SCRIPT_FILE
fi

export ATTESTATION_HUB_PORT_HTTP=${ATTESTATION_HUB_PORT_HTTP:-19082}
export ATTESTATION_HUB_PORT_HTTPS=${ATTESTATION_HUB_PORT_HTTPS:-19445}
export ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH=${ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH:-/etc/tenantconfig}
export ATTESTATION_HUB_POLL_INTERVAL=${ATTESTATION_HUB_POLL_INTERVAL:-2}
export ATTESTATION_HUB_DB_USERNAME=${ATTESTATION_HUB_DB_USERNAME:-root}
export ATTESTATION_HUB_DB_NAME=${ATTESTATION_HUB_DB_NAME:-attestation_hub_pu}
export ATTESTATION_HUB_DB_HOSTNAME=${ATTESTATION_HUB_DB_HOSTNAME:-localhost}
export ATTESTATION_HUB_DB_PORTNUM=${ATTESTATION_HUB_DB_PORTNUM:-5432}
export ATTESTATION_HUB_DB_DRIVER=${ATTESTATION_HUB_DB_DRIVER:-org.postgresql.Driver}
export MTWILSON_SERVER_PORT=${MTWILSON_SERVER_PORT:-8443}
export POSTGRESQL_KEEP_PGPASS=${POSTGRESQL_KEEP_PGPASS:-true}
export ATTESTATION_HUB_LOGS=${ATTESTATION_HUB_LOGS:-%{attestation_hub_log}}
export POSTGRES_LOG="$ATTESTATION_HUB_LOGS/intel.postgres.err"

# store directory layout in env file
echo "# $(date)" > %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_HOME=$ATTESTATION_HUB_HOME" >> %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_CONFIGURATION=$ATTESTATION_HUB_CONFIGURATION" >> %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_REPOSITORY=$ATTESTATION_HUB_REPOSITORY" >> %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_JAVA=$ATTESTATION_HUB_JAVA" >> %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_BIN=$ATTESTATION_HUB_BIN" >> %{attestation_hub_env}/attestation-hub-layout
echo "export ATTESTATION_HUB_LOGS=$ATTESTATION_HUB_LOGS" >> %{attestation_hub_env}/attestation-hub-layout


# store attestation-hub username in env file
echo "# $(date)" > %{attestation_hub_env}/attestation-hub-username
echo "export ATTESTATION_HUB_USERNAME=%{attestation_hub_username} >> %{attestation_hub_env}/attestation-hub-username"

# store the auto-exported environment variables in env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > %{attestation_hub_env}/attestation-hub-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name=$env_file_var_value" >> %{attestation_hub_env}/attestation-hub-setup
done


# if properties file exists
ATTESTATION_HUB_PROPERTIES_FILE=${ATTESTATION_HUB_PROPERTIES_FILE:-"%{attestation_hub_conf}/attestation-hub.properties"}

if [ -e $ATTESTATION_HUB_PROPERTIES_FILE ]; then
	attestation-hub export-config --in=%{attestation_hub_conf}/attestation-hub.properties --out=%{attestation_hub_conf}/attestation-hub.properties
fi

touch "$ATTESTATION_HUB_PROPERTIES_FILE"
chown "%{attestation_hub_username}":"%{attestation_hub_username}" "$ATTESTATION_HUB_PROPERTIES_FILE"
chmod 600 "$ATTESTATION_HUB_PROPERTIES_FILE"

# load existing environment; set variables will take precendence
load_attestation_hub_conf
load_attestation_hub_defaults


# prompt for installation variables if they are not provided
export ATTESTATION_HUB_DB_PASSWORD=${ATTESTATION_HUB_DB_PASSWORD:-$(generate_password 16)} 
#required database properties
prompt_with_default ATTESTATION_HUB_DB_NAME "Attestation-Hub db name:" "$ATTESTATION_HUB_DB_NAME"
update_property_in_file "attestation-hub.db.name" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_NAME"

prompt_with_default ATTESTATION_HUB_DB_HOSTNAME "Attestation-Hub db Hostname:" "$ATTESTATION_HUB_DB_HOSTNAME"
update_property_in_file "attestation-hub.db.hostname" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_HOSTNAME"

prompt_with_default ATTESTATION_HUB_DB_PORTNUM "Attestation-Hub db Portno:" "$ATTESTATION_HUB_DB_PORTNUM"
update_property_in_file "attestation-hub.db.portnum" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_PORTNUM"

update_property_in_file "attestation-hub.db.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_DB_USERNAME"

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

# modifying after mtwilson api client built
prompt_with_default MTWILSON_SERVER "Mtwilson Server:" "$MTWILSON_SERVER"
update_property_in_file "mtwilson.server" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER"

prompt_with_default MTWILSON_SERVER_PORT "Mtwilson Server Port:" "$MTWILSON_SERVER_PORT"
update_property_in_file "mtwilson.server.port" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_SERVER_PORT"

prompt_with_default MTWILSON_USERNAME "Mtwilson Username:" "$MTWILSON_USERNAME"
update_property_in_file "mtwilson.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_USERNAME"

prompt_with_default_password MTWILSON_PASSWORD "Mtwilson Password:" "$MTWILSON_PASSWORD"
update_property_in_file "mtwilson.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_PASSWORD"

export MTWILSON_API_URL=https://${MTWILSON_SERVER}:${MTWILSON_SERVER_PORT}/mtwilson/v2
prompt_with_default MTWILSON_API_URL "Mtwilson API Url:" "$MTWILSON_API_URL"

update_property_in_file "mtwilson.api.url" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_API_URL"

update_property_in_file "mtwilson.api.username" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_USERNAME"

update_property_in_file "mtwilson.api.password" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_PASSWORD"

prompt_with_default MTWILSON_TLS "Mtwilson TLS:" "$MTWILSON_TLS"
update_property_in_file "mtwilson.api.tls.policy.certificate.sha384" "$ATTESTATION_HUB_PROPERTIES_FILE" "$MTWILSON_TLS"

prompt_with_default ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH "Tenant Configurations Path:" "$ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH"
update_property_in_file "tenant.configuration.path" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_TENANT_CONFIGURATIONS_PATH"

prompt_with_default ATTESTATION_HUB_POLL_INTERVAL "Attestation Hub Poll Interval:" "$ATTESTATION_HUB_POLL_INTERVAL"
update_property_in_file "attestation-hub.poll.interval" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_POLL_INTERVAL"

update_property_in_file "attestation-hub.saml.timeout" "$ATTESTATION_HUB_PROPERTIES_FILE" "$ATTESTATION_HUB_SAML_TIMEOUT"

JAVA_CMD=$(type -p java | xargs readlink -f)
JAVA_HOME=$(dirname $JAVA_CMD | xargs dirname | xargs dirname)
JAVA_REQUIRED_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')

echo "# $(date)" > %{attestation_hub_env}/attestation-hub-java
echo "export JAVA_HOME=$JAVA_HOME" >> %{attestation_hub_env}/attestation-hub-java
echo "export JAVA_CMD=$JAVA_CMD" >> %{attestation_hub_env}/attestation-hub-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> %{attestation_hub_env}/attestation-hub-java

if [ -f "${JAVA_HOME}/jre/lib/security/java.security" ]; then
  echo "Replacing java.security file, existing file will be backed up"
  backup_file "${JAVA_HOME}/jre/lib/security/java.security"
  cp %{attestation_hub_home}/backup/java.security "${JAVA_HOME}/jre/lib/security/java.security"
fi


#postgresql configurations need to add
#detect postgres, install if not installed

#export postgres_required_version=${POSTGRES_REQUIRED_VERSION:-9.4}

postgres_write_connection_properties "$ATTESTATION_HUB_CONFIGURATION/attestation-hub.properties" attestation-hub.db
 
postgres_installed=1
touch ${ATTESTATION_HUB_HOME}/.pgpass
chmod 0600 ${ATTESTATION_HUB_HOME}/.pgpass
chown ${ATTESTATION_HUB_USERNAME}:${ATTESTATION_HUB_USERNAME} ${ATTESTATION_HUB_HOME}/.pgpass
export POSTGRES_HOSTNAME POSTGRES_PORTNUM POSTGRES_DATABASE POSTGRES_USERNAME POSTGRES_PASSWORD
if [ "$POSTGRES_HOSTNAME" == "127.0.0.1" ] || [ "$POSTGRES_HOSTNAME" == "localhost" ]; then
  PGPASS_HOSTNAME=localhost
else
  PGPASS_HOSTNAME="$POSTGRES_HOSTNAME"
fi
echo "$POSTGRES_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" > ${ATTESTATION_HUB_HOME}/.pgpass
echo "$PGPASS_HOSTNAME:$POSTGRES_PORTNUM:$POSTGRES_DATABASE:$POSTGRES_USERNAME:$POSTGRES_PASSWORD" >> ${ATTESTATION_HUB_HOME}/.pgpass
if [ $(whoami) == "root" ]; then cp ${ATTESTATION_HUB_HOME}/.pgpass ~/.pgpass; fi

postgres_server_rpm %{attestation_hub_conf}

if [ -z "$SKIP_DATABASE_INIT" ]; then
    # postgres db init here
	postgres_create_database
    if [ $? -ne 0 ]; then
      echo_failure "Cannot create database"
      exit 1
    fi
    #postgres_restart >> $INSTALL_LOG_FILE
    #sleep 10
    #export is_postgres_available postgres_connection_error
    if [ -z "$is_postgres_available" ]; then
      echo_warning "Run 'attestation-hub setup' after a database is available"; 
    fi
    # postgress db init end
else
  echo_warning "Skipping init of database"
fi

if [ $postgres_installed -eq 0 ]; then
  postgres_server_detect
  has_local_postgres_peer=`grep "^local.*all.*postgres.*peer" $postgres_pghb_conf`
  if [ -z "$has_local_postgres_peer" ]; then
    echo "Adding PostgreSQL local 'peer' authentication for 'postgres' user..."
    sed -i '/^.*TYPE.*DATABASE.*USER.*ADDRESS.*METHOD/a local all postgres peer' $postgres_pghb_conf
  fi
  has_local_peer=`grep "^local.*all.*all.*peer" $postgres_pghb_conf`
  if [ -n "$has_local_peer" ]; then
    echo "Replacing PostgreSQL local 'peer' authentication with 'md5' authentication..."
    sed -i 's/^local.*all.*all.*peer/local all all md5/' $postgres_pghb_conf
  fi
  if [ "$POSTGRESQL_KEEP_PGPASS" != "true" ]; then
    if [ -f ${ATTESTATION_HUB_HOME}/.pgpass ]; then
      echo "Removing .pgpass file to prevent insecure database password storage in plaintext..."
      rm -f ${ATTESTATION_HUB_HOME}/.pgpass
      if [ $(whoami) == "root" ]; then rm -f ~/.pgpass; fi
    fi
  fi
fi


ATTESTATION_HUB_PORT_HTTP=${ATTESTATION_HUB_PORT_HTTP:-${JETTY_PORT:-80}}
ATTESTATION_HUB_PORT_HTTPS=${ATTESTATION_HUB_PORT_HTTPS:-${JETTY_SECURE_PORT:-443}}

ln -s %{_sbindir}/attestation-hub.sh %{_sbindir}/attestation-hub

#attestation-hub setup update-extensions-cache-file --force 2> /dev/null

# add attestation-hub to startup services
ATTESTATION_HUB_PID_FILE=${ATTESTATION_HUB_PID_FILE:-%{attestation_hub_home}/attestation-hub.pid}
HUB_SCHEDULER_PID_FILE=${HUB_SCHEDULER_PID_FILE:-%{attestation_hub_home}/hubscheduler.pid}
if [ ! -w "$ATTESTATION_HUB_PID_FILE" ] && [ ! -w $(dirname "$ATTESTATION_HUB_PID_FILE") ]; then
  ATTESTATION_HUB_PID_FILE=$ATTESTATION_HUB_REPOSITORY/attestation-hub.pid
fi

register_startup_script %{attestation_hub_home}/bin/attestation-hub.sh attestation-hub $ATTESTATION_HUB_PID_FILE

disable_tcp_timestamps

# look for ATTESTATION_HUB_PASSWORD environment variable; if not present print help message and exit:
# Attestation Hub requires a master password
# losing the master password will result in data loss
# setup the attestation-hub, unless the NOSETUP variable is defined
if [ -z "$ATTESTATION_HUB_NOSETUP" ]; then
  attestation-hub setup
fi
rm -f ~/.pgpass

# ensure the attestation-hub owns all the content created during setup
for directory in %{attestation_hub_home} %{attestation_hub_conf} %{attestation_hub_env} %{attestation_hub_log}; do
  chown -R %{attestation_hub_username}:%{attestation_hub_username} $directory
done 
	
# Log Rotate
if [ ! -a %{_sysconfdir}/logrotate.d/attestation-hub ]; then
 echo "%{buildroot}/%{_localstatedir}/log/%{name}.log {
    missingok
	notifempty
	rotate %{attestation_hub_log_old}
	maxsize %{attestation_hub_log_size}
    nodateext
	%{attestation_hub_log_rotation_period}
	%{attestation_hub_log_compress}
	%{attestation_hub_log_delaycompress}
	%{attestation_hub_log_copytruncate}
}" > %{_sysconfdir}/logrotate.d/attestation-hub
fi
	
# attestation-hub start
# start the server, unless the NOSETUP variable is defined
if [ -z "$ATTESTATION_HUB_NOSETUP" ]; then attestation-hub start; fi
echo "Installation complete"

hash -r attestation-hub >/dev/null 2>&1


%clean
rm -rf %{buildroot}

%files
%defattr(0755,%{attestation_hub_username},%{attestation_hub_username},0700)

%{attestation_hub_conf}
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_conf}

%{attestation_hub_home}
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}

%{attestation_hub_home}/backup

%{attestation_hub_java}
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_java}

%{attestation_hub_home}/features
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/features

%{attestation_hub_home}/repository
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/repository

%{attestation_hub_home}/scripts
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/scripts

%{attestation_hub_home}/features/com.intel.mtwilson.core.login.token
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/features/com.intel.mtwilson.core.login.token

%{attestation_hub_home}/features/servlet3
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/features/servlet3

%{attestation_hub_home}/features/mtwilson-configuration-settings-ws-v2
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/features/mtwilson-configuration-settings-ws-v2

%{attestation_hub_tenantconfig}
%attr(0755, %{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_tenantconfig}

%attr(0700,%{attestation_hub_username},%{attestation_hub_username}) %{_sbindir}/attestation-hub.sh
%attr(0700,%{attestation_hub_username},%{attestation_hub_username}) %{_sbindir}/attestation-hub.bat
%attr(0666,%{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_log}/attestation-hub.log

%attr(0700,%{attestation_hub_username},%{attestation_hub_username}) %{_sbindir}/attestation-hub-functions.sh
%attr(0700,%{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/scripts/functions.sh
%attr(0700,%{attestation_hub_username},%{attestation_hub_username}) %{attestation_hub_home}/scripts/attestation-hub-functions.sh


%preun

. %{attestation_hub_home}/scripts/functions.sh

attestation-hub uninstall

%postun

if [ -d %{attestation_hub_conf} ]; then
   rm -rf %{attestation_hub_conf}
fi

if [ -d %{attestation_hub_home} ]; then
   rm -rf %{attestation_hub_home}
fi

if [ -d %{attestation_hub_tenantconfig} ]; then
   rm -rf %{attestation_hub_tenantconfig}
fi

%changelog
* Thu Sep 12 2019 Ananthamoorthi <ananthamoorthix.subramanian@intel.com>
- First release of Attestation-Hub RPM.

