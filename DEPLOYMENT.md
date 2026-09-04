# Cloud Deployment Guide - WhatsApp Nutrition Assistant

## Recommended Free / Low-Cost Cloud Hosts:
1. **Render.com** (Free Web Service + Managed PostgreSQL)
2. **Railway.app** (1-Click Docker deployment + PostgreSQL)
3. **AWS / DigitalOcean / Hetzner VPS** (Docker container + Nginx reverse proxy)

---

## 1. Deploying via Docker (Any Host)
The project includes a production-ready multi-stage `Dockerfile`:
```bash
# 1. Build the Docker image
docker build -t whatsapp-nutrition-bot .

# 2. Run container with environment variables
docker run -d -p 8085:8085 \
  -e SERVER_PORT=8085 \
  -e WHATSAPP_VERIFY_TOKEN=my_access_token_2026 \
  -e META_ACCESS_TOKEN=your_permanent_meta_token \
  -e META_PHONE_NUMBER_ID=1219522864587242 \
  -e GEMINI_API_KEY=your_gemini_key \
  -e GEMINI_API_KEY_SECONDARY=your_secondary_gemini_key \
  -e APP_PUBLIC_BASE_URL=https://your-public-domain.com \
  --name nutrition-bot whatsapp-nutrition-bot
```

---

## 2. Meta WhatsApp Production Setup:
1. **System User Permanent Token**:
   - In Meta Business Suite -> Settings -> System Users -> Generate New Token.
   - Select permissions: `whatsapp_business_messaging`, `whatsapp_business_management`.
   - Set expiration to **Never** so you never have to refresh tokens!
2. **Webhook URL**:
   - Set Callback URL to `https://<your-deployed-domain>/webhook`.
   - Verify token: `my_access_token_2026`.
   - Subscribe to field: `messages`.
