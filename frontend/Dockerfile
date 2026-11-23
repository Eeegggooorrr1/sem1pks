FROM node:22-alpine AS dependencies
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM dependencies AS test
COPY . .
CMD ["npm", "run", "check"]

FROM dependencies AS build
COPY . .
RUN npm run build

FROM nginx:stable-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

FROM mcr.microsoft.com/playwright:v1.65.0-noble AS e2e
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
CMD ["npm", "run", "test:e2e"]
