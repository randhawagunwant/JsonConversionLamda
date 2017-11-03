#!/bin/bash

#
# usage: launch-tower.job.sh <asset> <env> <version> <job_template_id>
#

#
#exec 3>&1
#exec 1>&2


script_input='/tmp/input'

asset=$1
env=$2
version=$3
job_template_id=$4
tower_creds=$5

echo "$asset version is $version"

if [ $env == "pine" ]; then
  ansible_tower_url="https://tower.cops.collegeboard.org"
  ansible_tower_project="20"
  ansible_tower_organization="3"
  ansible_tower_scm_credential="8"
else
  ansible_tower_url="https://tower-nonprod.cops.collegeboard.org"
  ansible_tower_project="8"
  ansible_tower_organization="2"
  ansible_tower_scm_credential="5"
fi

echo "tower url: $ansible_tower_url"

if [[ $version == 9.9.9.* ]]; then
  branch="master"
else
  branch="$asset-$version"
fi

echo "branch: $branch"

rm -f $script_input

echo "making PUT requst to Update project branch to $asset-$version..."

update_response=$(curl -k -s -X PUT -H "Content-Type:application/json" -H "Authorization: Basic $tower_creds" -d '{"name": "dmfcloud","organization":"'"$ansible_tower_organization"'", "scm_type": "git","scm_url": "https://bitbucket.collegeboard.org/scm/dmf/dmfcloud.git","scm_branch":"'"$branch"'","credential":"'"$ansible_tower_scm_credential"'","scm_update_on_launch": true}'  $ansible_tower_url/api/v1/projects/$ansible_tower_project/ )

echo "$update_response"


echo 'making request POST requst to Tower API ...'

launch_response=$(curl -k -s -X POST -H "Content-Type:application/json" -H "Authorization: Basic $tower_creds" -d '{"extra_vars": "{\"asset\": \"'"$asset"'\", \"env\": \"'"$env"'\", \"version\": \"'"$version"'\"}"}'  $ansible_tower_url/api/v1/job_templates/$job_template_id/launch/ )

echo "$launch_response"

id=$(echo $launch_response | jq -r '.id')
template=$(echo $launch_response | jq -r '.summary_fields.job_template.name')
project=$(echo $launch_response | jq -r '.summary_fields.project.name')
playbook=$(echo $launch_response | jq -r '.playbook')

#get the path to stdout API for this job execution
job_path=$(echo $launch_response | jq -r '.url')


status=$(echo $launch_response | jq -r '.status')

echo 'making stdout GET request to Tower API'
printf "\n$status"
while [[ $status != "successful" ]] && [[ $status != "failed" ]]
do
  job_response=$(curl -k -s -X GET -H "Content-Type:application/json" -H "Authorization: Basic $tower_creds"  $ansible_tower_url$job_path -d '{}')
  new_status=$(echo $job_response | jq -r '.status')
  if [[ $new_status != $status ]]; then
    printf "\n$new_status"
    status=$new_status
  fi
  printf "."
done

result_stdout=$(echo $job_response | jq -r '.result_stdout')
echo "$result_stdout"

  # echo '{"version": { "id": "'$id'"},
  #      "metadata": [{"name": "job", "value": "'$id'"},
  #                   {"name": "status", "value": "'$status'"},
  #                   {"name": "server", "value": "'$tower_baseurl'"},
  #                   {"name": "project", "value": "'$project'"},
  #                   {"name": "job_template_name", "value": "'$template'"},
  #                   {"name": "asset", "value": "'$asset'"},
  #                   {"name": "env", "value": "'$env'"},
  #                   {"name": "version", "value": "'$version'"},
  #                   {"name": "playbook", "value": "'$playbook'"}]}' >&3
if [ $status != "successful" ]; then
  exit 1
else
  exit 0
fi
