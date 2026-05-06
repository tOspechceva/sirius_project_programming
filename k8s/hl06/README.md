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

## 3) Apply manifests

```bash
kubectl apply -f k8s/hl06/00-namespace.yaml
kubectl apply -f k8s/hl06/01-configmap.yaml
kubectl apply -f k8s/hl06/03-app-deployment-and-services.yaml
kubectl apply -f k8s/hl06/04-additional-deployment-and-services.yaml
kubectl get all -n hl06 -o wide
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
- app: `http://<NODE_IP>:30082/swagger-ui/index.html`
- additional: `http://<NODE_IP>:30083/swagger-ui/index.html`
