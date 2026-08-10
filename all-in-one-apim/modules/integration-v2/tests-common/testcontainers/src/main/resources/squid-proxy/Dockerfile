FROM alpine:3.20

# squid: the proxy; apache2-utils: htpasswd for auth credential file
RUN apk add --no-cache squid apache2-utils

# Credentials match the original WebSocketProxyProfileTestCase constants.
# Two Squid instances run in one container:
#   port 3128 — anonymous (no Proxy-Authorization required)
#   port 3129 — authenticated (Proxy-Authorization: Basic testproxyuser:testproxypass required)
RUN htpasswd -bc /etc/squid/htpasswd testproxyuser testproxypass

# Separate cache and coredump dirs per instance so the two Squid processes don't collide.
RUN mkdir -p /var/log/squid \
             /var/cache/squid-anon \
             /var/cache/squid-auth \
    && chown -R squid:squid /var/log/squid \
                            /var/cache/squid-anon \
                            /var/cache/squid-auth

COPY squid-anon.conf /etc/squid/squid-anon.conf
COPY squid-auth.conf /etc/squid/squid-auth.conf
COPY entrypoint.sh   /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 3128 3129

ENTRYPOINT ["/entrypoint.sh"]
