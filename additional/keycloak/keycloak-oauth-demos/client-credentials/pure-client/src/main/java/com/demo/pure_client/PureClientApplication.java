package com.demo.pure_client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class PureClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(PureClientApplication.class, args);
	}

    /*
        Creates a single RestTemplate managed by Spring so you can inject it anywhere (like in your CommandLineRunner).
     */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

    /*
        ClientRegistrationRepository is a Spring Security component that holds all OAuth2 client definitions
         (client id, secret, token endpoint, scopes, etc.).
         Spring Boot creates it automatically when it sees OAuth2 client configuration:
         "spring.security.oauth2.client.registration.*"

         That repository contains OAuth2 client definitions from your config, e.g.:
         token endpoint
         client id
         client secret
         scopes
         issuer / realm
         etc.

        auth2AuthorizedClientService:
         This service stores authorized clients (which includes the access token + metadata), keyed by:
            clientRegistrationId (e.g. "keycloak-client")
            principalName (e.g. "machine")
        InMemory... means:
            stored only in application memory
            lost on restart
            suitable for demos / simple services
     */
	@Bean
	public OAuth2AuthorizedClientService auth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Bean
	public OAuth2AuthorizedClientManager auth2AuthorizedClientManager(
			ClientRegistrationRepository repos,
			OAuth2AuthorizedClientService clientService
	) {
		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				repos, clientService
		);

		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
				.clientCredentials()
				.build();

		manager.setAuthorizedClientProvider(provider);
		return manager;

	}
    /*
    In Spring Boot, a CommandLineRunner is a hook interface that lets you run custom code once,
    right after the application context has fully started.
     */
	@Bean
	public CommandLineRunner run(
			OAuth2AuthorizedClientManager manager,
			RestTemplate rest,
			@Value("${service2.url}") String service2Url
	) {
        //we can schedule this piece of code to run like every hour for authentication.
		return args -> {
            /*
            Creating an OAuth2 authorization request
            This tells Spring Security:
            Which OAuth2 client to use
            → "keycloak-client" (from spring.security.oauth2.client.registration.*)
            Who is authenticating
            → "machine" (a technical / non-human principal)
️                This is machine-to-machine authentication, not user login.
             */
		var authRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId("keycloak-client")
				.principal("machine")
				.build();

        /*
        What happens here:
        Spring contacts Keycloak
        Performs OAuth2 flow (usually client_credentials)
        Receives an access token (JWT)
        Returns an OAuth2AuthorizedClient
        You extract the raw token string
        This is the Bearer token you’ll use for inter-service calls.
        */
		var client = manager.authorize(authRequest);
		String token = client.getAccessToken().getTokenValue();

        // Adding the Bearer token to HTTP headers

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);

        /*
            This sends: GET request
            To: http://service2-url/data
            With: Authorization: Bearer <token>
            Spring:
            performs the HTTP call
            deserializes the response body into a String
         */
		var resp = rest.exchange(
				service2Url + "/data",
				HttpMethod.GET,
				new HttpEntity<>(headers),
				String.class
		);

		System.out.println("Response from Service 2: " + resp.getBody());

	};
	}

}
