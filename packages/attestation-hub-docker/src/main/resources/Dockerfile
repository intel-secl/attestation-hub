#####
#Copyright © 2019 Intel Corporation
#SPDX-License-Identifier: BSD-3-Clause
#####

FROM registry.access.redhat.com/rhel7-init:7.5

# Copy yum repositories (If there is a non-redhat repo)
COPY yum.repos.d/ /etc/yum.repos.d/

#RUN yum makecache
RUN yum -y install which sudo redhat-lsb-core systemd openssl

# Install Postgres 9.4 client.
RUN yum install https://download.postgresql.org/pub/repos/yum/9.4/redhat/rhel-7-x86_64/pgdg-redhat94-9.4-3.noarch.rpm -y && \
    yum install -y postgresql94

# Copy the scripts
COPY attestation-hub*.bin /root/attestation_hub.bin
RUN chmod +x /root/attestation_hub.bin

# Copy the installer and extract just the static components 
WORKDIR /root/

# Install Verification Service
RUN export ATTESTATION_HUB_NOSETUP=yes && export SKIP_DATABASE_INIT=yes && export ATTESTATION_HUB_DB_HOSTNAME='running_on_remote' && /root/attestation_hub.bin &> /root/install.log

# Remove the installer bin
RUN rm /root/attestation_hub.bin

COPY start_ahub /root/
RUN chmod +x /root/start_ahub

ENTRYPOINT [ "/root/start_ahub" ]
