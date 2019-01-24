# Kubernetes Demo
*The goal of this repository is to automate the deployment environment for deployment pattern 1,deployment of WSO2 API Manager deployment pattern 1,automating sample backend service.*

### Prerequisites
-----

1.Create a [Google Cloud Platform Project.](https://console.cloud.google.com/projectselector/compute/instances)

2.In order for Ansible to create Compute Engine instances, you'll need a [Service Account](https://cloud.google.com/compute/docs/access/service-accounts#serviceaccount). Make sure to create a new JSON formatted private key file for this Service Account. Note the Email address of this Service Account should be YOUR_SERVICEACCOUNT@YOUR_PROJECT_ID.iam.gserviceaccount.com, since this will be required in the Ansible configuration files.

3.Next  install the [Cloud SDK](https://cloud.google.com/sdk/) and make sure you've successfully authenticated.

4.Set up SSH keys that will allow you to access your Compute Engine instances. You can either [manually generate the keys](https://cloud.google.com/compute/docs/instances/adding-removing-ssh-keys#createsshkeys) and paste the public key into the [metadata server](https://console.cloud.google.com/compute/metadata/sshKeys) or you can use gcloud compute ssh to access an existing Compute Engine instance and it will handle generating the keys and uploading them to the metadata server.
### Getting Started
------
**1.Deploying [WSO2 API Manager pattern-1]((https://github.com/wso2/kubernetes-apim/blob/master/pattern-1/README.md))**
    
 This deployment process would require steps   
 
*Step 1: Create a [NFS Server](https://cloud.google.com/marketplace/docs/single-node-fileserver) in gCloud*

*Step 2: Create a Kubernetes Cluster in gCloud*

*Step 3: Deploy WSO2 API Manager*


**2.Deploying Backend service**

## **1.Deploying WSO2 API Manager pattern-1**


 **Step 1: Create a [NFS Server](https://cloud.google.com/marketplace/docs/single-node-fileserver) in gCloud**
- A pre-configured Network File System (NFS) to be used as the persistent volume for artifact sharing and persistence. In the NFS server instance, create a Linux system user account named `wso2carbon` with user id `802` and a system group named `wso2` with group id `802`. Add the `wso2carbon` user to the group `wso2`.
  ```
  sudo groupadd --system -g 802 wso2
  ```
  ```
  sudo useradd --system -g 802 -u 802 wso2carbon
  ```
- Create unique directories within the NFS server instance for each Kubernetes Persistent Volume.
  ```
  mkdir /data/<directory_name>
  ```
  ```
  Mkdir /data/<directory_name_mysql>
  ```

- Grant ownership to `wso2carbon` user and `wso2` group, for each of the previously created directories.
  ```
  sudo chown -R wso2carbon:wso2  <directory_name>
  ```
- Grant read-write-execute permissions to the `wso2carbon` user, for each of the previously created directories. 
  ```
  sudo chmod -R 700 <directory_name>
  ```
  
**Step 2: Create a [Kubernetes Cluster](https://docs.ansible.com/ansible/latest/modules/gcp_container_cluster_module.html) in gCloud**

   *Step 2.1: Create a GC service account with credentials and download JSON credential*
    
   *Step 2.2: Create An VM instance in Google Compute Engine*
    
   *Step 2.3: Provide Credentials as Module Parameters for Ansible*
    
   *Step 2.4: Create a host Inventory file*
    
   *Step 2.5: Testing the Playbook*
    
   *Step 2.6: Creating the kubernetes cluster by executing the Playbook*
    
    
   **Step 2.2: Create A [VM instance](https://cloud.google.com/compute/docs/instances/create-start-instance) in Google Compute Engine**
   
 Requirements
  1. python >= v2.6,
  2. requests >= v2.18.4
  3. google-auth >= v1.3.0
  4. ansible<=v2.7

- Install Python
- Install [Ansible](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html)
- Install libcloud
  ```
  sudo pip install apache-libcloud==0.20.1
  ```
- Install request and google-auth
  ```
  pip install requests google-auth
  ```
**Step 2.3: Provide Credentials as Module Parameters for ansible**
(refer the gce_cluster folder )
- Edit `gce/gce_cluster/cluster_roles/gke/cluster_vars/main.yml`  file and specify your Project ID in the project_id variable, Service Account email address in the service_account_email variable, and the location of your JSON key. (downloaded earlier) 
    
**Step 2.4: Create a host inventory file**
- Edit /etc/ansible/hosts.
  ```
  [local]
  <INSTANCE_NAME> ansible_user=<USER> ansible_ssh_private_key_file=<FILE_PATH_TO_SSH_KEY>
   ```
**Step 2.5: Testing the playbook**
- Use the --syntax-check and -list-tasks options to do a full syntax check.(errors will be show up in red)
   ```
  ansible-playbook --syntax-check --list-tasks -i <inventory file path>./<playbook yml>
   ```
**Step 2.6: Creating the kubernetes cluster by executing the playbook**
- Execute the `cluster_site.yml`. (gce/gke_cluster/cluster_site.yml)
  ```
  ansible-playbook  -i etc/ansible/hosts ./cluster_site.yml
  ```
 **Step 3: Deploy WSO2 API Manager**
- Clone the Kubernetes Resources for WSO2 API Manager Git repository.
   ```
   git clone https://github.com/wso2/kubernetes-apim.git
   ```
- Update the Kubernetes Persistent Volume resource with the corresponding NFS server IP (NFS_SERVER_IP) and exported, NFS server directory path (NFS_LOCATION_PATH) in

    `kubernetes-apim-2.6x/pattern-1/extras/rdbms/volumes/persistent-volumes.yaml`
    
    `kubernetes-apim-2.6x/pattern-1/volumes/persistent-volumes.yaml`

- Execute `deploy.sh` in `kubernetes-apim-2.6.x/pattern-1/scripts` with *Kubernetes cluster admin password*.
  ```
  ./deploy.sh --wu=<wso2 username> --wp=<wso2 password> --cap=<Kubernetes cluster admin password>
  ```
  
  
## **2.Deploying Backend service**

- Execute `ingress-deploy.sh` with Kubernetes cluster admin password.
(Update the <PATH_FOR_THE_SERVICE_YML_FILE> with the backend service’s service file yml and <PATH_FOR_THE_SERICE_DEPLOYMENT_YML_FILE> with the backend service deployment file
  ```
  ./service-deploy.sh --cap=<Kubernetes cluster admin password>
  ```




















