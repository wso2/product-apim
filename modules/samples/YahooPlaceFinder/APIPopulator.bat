set SERVER=http://localhost:9763
curl -X POST -c cookies %SERVER%/publisher/site/blocks/user/login/ajax/login.jag -d "action=login&username=provider1&password=provider1"
curl -X POST -b cookies %SERVER%/publisher/site/blocks/item-add/ajax/add.jag -d "action=addAPI&name=PlaceFinder&version=1.0.0&description=Place Finder&endpoint=http://where.yahooapis.com/geocode&wsdl=&tags=finder,open,social&tier=Silver&thumbUrl=http://images.ientrymail.com/webpronews/article_pics/yahoo-placefinder.jpg&context=/placeFinder&tiersCollection=Gold&resourceCount=0&resourceMethod-0=GET&resourceMethodAuthType-0=Application&uriTemplate-0=/*"
curl -X POST -b cookies %SERVER%/publisher/site/blocks/life-cycles/ajax/life-cycles.jag -d "name=PlaceFinder&version=1.0.0&provider=provider1&status=PUBLISHED&publishToGateway=true&action=updateStatus"




