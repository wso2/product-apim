var result;

requirejs.config(
    {
        baseUrl: basePath,
        paths: {
            lib: "lib",
            lodash: "lib/lodash_v4.13.1/lodash",
            app: "js",
            log4javascript: "lib/log4javascript_custom/log4javascript",
            event_channel: "js/event/channel",
            log: "js/log_custom/log",
            ace: "lib/ace_v1.2.6",
            constants: 'js/constants/constants'
        },
        packages: [
            {
                name: 'ballerina',
                location: 'js/ballerina',
                main: 'module'
            }
        ],
        waitSeconds: 2
    }
);

require( ["app/ballerina/ast/module","app/ballerina/visitors/main","app/ballerina/visitors/source-gen/module"], function(AST,VISITORS,GEN) {

        var model = AST.BallerinaASTDeserializer.getASTModel(JSON.parse(jsonModel));
        var sourceGenVisitor = new GEN.BallerinaASTRootVisitor();
        model.accept(sourceGenVisitor);
        result = sourceGenVisitor.getGeneratedSource();
    }
);