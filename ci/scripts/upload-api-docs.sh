#!/bin/sh

if [ -z "${EAAS_API_DOCS_SSH_KEY}" ] ; then
	echo 'Skipping API docs upload! No SSH key defined.'
	exit 0
fi

__info() {
	echo "--> $1"
}

set -eu

# Prepare SSH for working with Git repositories

which ssh-agent > /dev/null
if [ "$?" -ne 0 ] ; then
	__info 'Installing openssh-client...'
	apt-get update -y
	apt-get install openssh-client -y
fi

__info 'Importing private key...'
mkdir -p ~/.ssh
chmod 700 ~/.ssh
eval $(ssh-agent -s) > /dev/null
echo "${EAAS_API_DOCS_SSH_KEY}" | tr -d '\r' | ssh-add -

__info 'Updating known-hosts...'
echo "${SSH_KNOWN_HOSTS}" > ~/.ssh/known_hosts
chmod 644 ~/.ssh/known_hosts

# Update repository with generated pages

docsrc="$1"
docdir="/tmp/eaas-api-docs"
doctgt="${docdir}/public/${CI_COMMIT_REF_NAME}"

__info 'Cloning api-docs repository...'
mkdir -p "${docdir}" && cd "${docdir}"
git clone "${EAAS_API_DOCS_REPO_URL}" .

__info 'Updating api-docs pages...'
mkdir -p "${doctgt}"
rm -r ${doctgt}/* || true
cp -r ${docsrc}/* "${doctgt}"

__info 'Commiting changes...'
git config --local user.email "nobody@openslx.com"
git config --local user.name "eaas-server-ci"
git add public/
git commit -m "Update triggered by pipeline ${CI_PIPELINE_URL}" || true

__info 'Pushing changes...'
git push origin master

