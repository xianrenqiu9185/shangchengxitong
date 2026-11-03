# ============================================================
# 邻里鲜生 · C 端商城（Vue3 + Vite → Nginx）
# 构建：docker build -t linli-web-mall:local frontend/linli-mall-web
# 说明：基础镜像默认走 ECR 公共镜像站，可用 build-arg 覆盖（CI 用 Docker Hub）
# ============================================================
ARG NODE_IMAGE=public.ecr.aws/docker/library/node:20-alpine
ARG NGINX_IMAGE=public.ecr.aws/docker/library/nginx:1.27-alpine

FROM ${NODE_IMAGE} AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM ${NGINX_IMAGE}
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
