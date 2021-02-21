package org.wso2.am.integration.tests.listener;

import org.apache.commons.lang.StringUtils;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestFilter implements IMethodInterceptor {
    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> list, ITestContext iTestContext) {
        String testsToRunCommaSeparated = System.getenv("PRODUCT_APIM_TESTS");
        if (StringUtils.isBlank(testsToRunCommaSeparated)) {
            return list;
        }
        List<String> testList = Arrays.asList(testsToRunCommaSeparated.split(","));
        if (testList.contains(iTestContext.getName())) {
            return list;
        }
        return new ArrayList<>();
    }
}
