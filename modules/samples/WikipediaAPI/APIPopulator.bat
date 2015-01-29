set SERVER=http://localhost:9763
curl -X POST -c cookies %SERVER%/publisher/site/blocks/user/login/ajax/login.jag -d "action=login&username=admin&password=admin"

curl -X POST -b cookies $SERVER/publisher/site/blocks/item-add/ajax/add.jag -d "action=addAPI&name=WikipediaAPI&visibility=public&version=1.0.0&description=If you want to monitor a MediaWiki installation, or create a bot to automatically maintain one, you can use the MediaWiki web service API. The web service API provides direct, high-level access to the data contained in MediaWiki databases&endpointType=nonsecured&http_checked=http&https_checked=https&wsdl=&tags=wikipedia,mediawiki&tier=Silver&thumbUrl=https://upload.wikimedia.org/wikipedia/en/b/bc/Wiki.png&context=/wikipedia&tiersCollection=Gold&resourceCount=0&resourceMethod-0=GET&resourceMethodAuthType-0=Application&resourceMethodThrottlingTier-0=Unlimited&uriTemplate-0=/*" -d 'endpoint_config={"production_endpoints":{"url":"http://en.wikipedia.org/w/api.php","config":null},"endpoint_type":"http"}';

curl -X POST -b cookies $SERVER/publisher/site/blocks/life-cycles/ajax/life-cycles.jag -d "name=WikipediaAPI&version=1.0.0&provider=admin&status=PUBLISHED&publishToGateway=true&action=updateStatus";


