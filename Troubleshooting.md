## Not able to read the environment variables on the host
export the env variables

## Not able to read the environment variables when executing the script remotely
ssh $user@$VM 'echo $CCNL_HOME' - prints nothing
make sure the environment variables are set in ~/.ssh/environment file and PermitUserEnvironment is set 
Read https://serverfault.com/questions/586382/environment-variables-are-unavailable-when-running-scripts-over-ssh/586393#586393
https://superuser.com/questions/136646/how-to-append-to-a-file-as-sudo

Note: need to restart the ssh service or the server to make this effective

## Array Index Out of bound Exception at line 312 getNodeStatus of placement service
Make sure there is no extra blank line in nodeInformation file!

