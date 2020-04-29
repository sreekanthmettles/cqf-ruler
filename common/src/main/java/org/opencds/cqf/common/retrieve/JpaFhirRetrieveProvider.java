package org.opencds.cqf.common.retrieve;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.retrieve.*;
import org.opencds.cqf.cql.searchparam.SearchParameterResolver;

import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.bundle.BundleEntryParts;
import java.util.Iterator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.common.config.HapiProperties;

public class JpaFhirRetrieveProvider extends SearchParamFhirRetrieveProvider {

    DaoRegistry registry;

    public JpaFhirRetrieveProvider(DaoRegistry registry, SearchParameterResolver searchParameterResolver) {
        super(searchParameterResolver);
        this.registry = registry;
    }

    @Override
    protected Iterable<Object> executeQueries(String dataType, List<SearchParameterMap> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> objects = new ArrayList<>();
        for (SearchParameterMap map : queries) {
            objects.addAll(executeQuery(dataType, map));
        }

        return objects;
    }

    protected Collection<Object> executeQuery(String dataType, SearchParameterMap map) {
        IFhirResourceDao<?> dao = this.registry.getResourceDao(dataType);
        FhirContext myFhirContext = FhirContext.forR4();
        int a = 3;
        if (HapiProperties.getProperties().containsKey("patient_server")) {
            if (HapiProperties.getProperties().getProperty("patient_server")
                    .equals("google")) {
                a = 2;
            }
            if (HapiProperties.getProperties().getProperty("patient_server")
                    .equals("hapi")) {
                a = 1;
            }
        }

        IGenericClient client;
        List<IBaseResource> resourceList = new ArrayList();
        if (a != 3) {
            String purl = HapiProperties.getProperties().getProperty("patient_server_url");
            client = FhirContext.forR4()
                    .newRestfulGenericClient(purl);

            if (a == 2) {


                /*client = FhirContext.forR4()
                    .newRestfulGenericClient("https://healthcare.googleapis.com/v1"
                            + "/projects/fhir-274221/locations/us-west2"
                            + "/datasets/sample/fhirStores/spl/fhir");*/
                String token = new GoogleAuthenticator().getToken();
                System.out.println("The query string is " + token);

                BearerTokenAuthInterceptor authInterceptor
                        = new BearerTokenAuthInterceptor(token);

                client.registerInterceptor(authInterceptor);

            }
            String searchURL = "/" + dataType + map.toNormalizedQueryString(myFhirContext);

            System.out.println("The query string is " + client.getServerBase() + searchURL);

            

            IBaseBundle bundle = client.search().byUrl(searchURL)
                    .cacheControl(new CacheControlDirective().setNoCache(true)).execute();
            List<BundleEntryParts> parts = BundleUtil.toListOfEntries(myFhirContext, bundle);
            for (Iterator<BundleEntryParts> iterator = parts.iterator(); iterator.hasNext();) {
                BundleEntryParts next = iterator.next();
                resourceList.add(next.getResource());
            }
            // IGenericClient client = FhirContext.forR4()
            //       .newRestfulGenericClient("http://localhost:9999/hapi-fhir-jpaserver/fhir");


            if (a == 3) {
                IBundleProvider bundleProvider = dao.search(map);
                if (bundleProvider.size() == null) {
                    return resolveResourceList(bundleProvider.getResources(0, 10000));
                }
                if (bundleProvider.size() == 0) {
                    System.out.println("Empty data ");
                    return new ArrayList<>();
                }
                resourceList = bundleProvider.getResources(0, bundleProvider.size());

            } else {

            }
        }

        return resolveResourceList(resourceList);
    }

    public synchronized Collection<Object> resolveResourceList(List<IBaseResource> resourceList) {
        List<Object> ret = new ArrayList<>();
        for (IBaseResource res : resourceList) {
            Class<?> clazz = res.getClass();
            ret.add(clazz.cast(res));
        }
        // ret.addAll(resourceList);
        return ret;
    }

}
