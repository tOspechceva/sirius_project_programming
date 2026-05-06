# K3s deployment (hl06)

## 1) Join hl06 VM to the cluster

```bash
# login to your VM
ssh -p 2306 hl@hlssh.zil.digital

# check your VPN IP
ip a show tun0

# install k3s agent
curl -sfL https://get.k3s.io | \
  K3S_URL=https://10.60.3.14:6443 \
  K3S_TOKEN='K1073c292e757496b71aac5e7f667879a6ad6cbba3d87d3ebf8918f3e9ec5e1b257::server:9bc0003b7c8d149ca35d44b84dce282d' \
  INSTALL_K3S_EXEC="--node-ip <HL06_TUN0_IP> --flannel-iface tun0" sh -

sudo systemctl status k3s-agent --no-pager
```

## 2) Create required secrets (manual)

```bash
# docker registry pull secret
kubectl create namespace hl06
kubectl create secret docker-registry dockerhub-registry-secret \
  --namespace hl06 \
  --docker-server=10.60.3.11:8888 \
  --docker-username='<REGISTRY_USER>' \
  --docker-password='<REGISTRY_PASSWORD>' \
  --docker-email='noreply@example.com'

# DB password secret
kubectl create secret generic hl06-db-secret \
  --namespace hl06 \
  --from-literal=DB_PASSWORD='hl_postgres'
```

Templates for Git (do not apply as-is with placeholder values):
- `k8s/hl06/common/02-db-secret-template.yaml`
- `k8s/hl06/common/03-registry-secret-template.yaml`

## 3) Apply manifests

```bash
# label allowed worker nodes for this workload
kubectl label node hl06 hl06-workload=true --overwrite
kubectl label node hl17 hl06-workload=true --overwrite

kubectl apply -f k8s/hl06/common/00-namespace.yaml
kubectl apply -f k8s/hl06/common/01-app-configmap.yaml
kubectl apply -f k8s/hl06/common/02-additional-configmap.yaml
kubectl apply -f k8s/hl06/common/04-resourcequota.yaml
kubectl apply -f k8s/hl06/app/01-lab-kafka-config.yaml
kubectl apply -f k8s/hl06/additional/01-lab-kafka-config.yaml
kubectl apply -f k8s/hl06/app/10-deployment.yaml
kubectl apply -f k8s/hl06/app/20-service-internal.yaml
kubectl apply -f k8s/hl06/app/21-service-nodeport.yaml
kubectl apply -f k8s/hl06/additional/10-deployment.yaml
kubectl apply -f k8s/hl06/additional/20-service-internal.yaml
kubectl apply -f k8s/hl06/additional/21-service-nodeport.yaml
kubectl get all -n hl06 -o wide
```

ResourceQuota check:
```bash
kubectl get resourcequota -n hl06
kubectl describe resourcequota hl06-quota -n hl06
```

## 4) Validation

```bash
# from VM
kubectl get pods -n hl06
kubectl get svc -n hl06

# port-forward for Swagger
kubectl -n hl06 port-forward svc/hl06-app-internal 18081:8081
kubectl -n hl06 port-forward svc/hl06-additional-internal 18082:8082
```

Swagger URLs:
- app: `http://localhost:18081/swagger-ui/index.html`
- additional: `http://localhost:18082/swagger-ui/index.html`

NodePort URLs (replace `<NODE_IP>` with any cluster node IP reachable from your client):
- app: `http://<NODE_IP>:31082/swagger-ui/index.html`
- additional: `http://<NODE_IP>:31083/swagger-ui/index.html`

## 5) k9s for demo

```bash
# install latest k9s
curl -sSL https://github.com/derailed/k9s/releases/latest/download/k9s_Linux_amd64.tar.gz -o /tmp/k9s.tar.gz
tar -xzf /tmp/k9s.tar.gz -C /tmp
sudo install -m 0755 /tmp/k9s /usr/local/bin/k9s

# run in your namespace
k9s -n hl06
```

Useful keys in k9s:
- `:po` pods
- `:svc` services
- `:deploy` deployments
- `l` logs
- `d` describe
- `s` shell in pod
