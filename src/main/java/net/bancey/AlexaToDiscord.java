package net.bancey;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.servlet.SpeechletServlet;
import net.bancey.services.DiscordApp;
import net.bancey.speechlets.DiscordSpeechlet;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bancey on 11/12/2016.
 */
@Controller
@SpringBootApplication
@EnableAutoConfiguration
public class AlexaToDiscord extends SpringBootServletInitializer {

    //Declare class fields
    private static DiscordApp discordInstance;
    private final String BASE_URL = "https://discordapp.com/api";
    private final String AUTH_URL = "https://discordapp.com/api/oauth2/authorize";
    private final String TOKEN_URL = BASE_URL + "/oauth2/token";
    private final String CALLBACK_URL = "https://alexa-discord.herokuapp.com/oauth/callback";
    private final String CLIENT_SECRET = "sGM8PzikQ5oqcrxm8cvxh2PSfWFvSxJa";
    private final String CLIENT_ID = "259260480105742337";
    private String state;
    private String redirectURI;

    /*
     * Constructor for the AlexaToDiscord class, this is the entry point of the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AlexaToDiscord.class, args);
        String token = System.getenv("DISCORD_TOKEN");
        AlexaToDiscord.discordInstance = new DiscordApp(token);
    }

    /*
     * Method that returns the instance of the DiscordApp class
     */
    public static DiscordApp getDiscordInstance() {
        return discordInstance;
    }

    /*
     * Method that maps the url / to the string "Alexa-Discord is running!" this allows you to see if the
     * application is running by navigating to the application in your browser
     */
    @RequestMapping("/")
    @ResponseBody
    public String home() {
        return "Alexa-Discord is running!";
    }

    @RequestMapping("/oauth/authorize")
    @ResponseBody
    public ModelAndView authorize(@RequestParam("client_id") String clientId, @RequestParam("state") String state, @RequestParam("redirect_uri") String redirectURI, HttpServletRequest req, HttpServletResponse res) {
        try {
            this.state = state;
            this.redirectURI = redirectURI;

            OAuthClientRequest request = OAuthClientRequest
                    .authorizationLocation(AUTH_URL)
                    .setClientId(clientId)
                    .setRedirectURI(CALLBACK_URL)
                    .setResponseType(ResponseType.CODE.toString())
                    .setScope("guilds")
                    .buildQueryMessage();
            System.out.println(request.getLocationUri());
            return new ModelAndView(new RedirectView(request.getLocationUri()));
        } catch (OAuthSystemException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping("/oauth/callback")
    @ResponseBody
    public ModelAndView callback(HttpServletRequest request, HttpServletResponse response) {
        try {

            OAuthAuthzResponse oar;
            oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oar.getCode();

            /*OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                    .tokenLocation(TOKEN_URL)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(CLIENT_ID)
                    .setClientSecret(CLIENT_SECRET)
                    .setRedirectURI(CALLBACK_URL)
                    .setCode(code)
                    .buildBodyMessage();

            oAuthClientRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

            System.out.println(oAuthClientRequest.getBody());
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

            OAuthAccessTokenResponse tokenResponse = oAuthClient.accessToken(oAuthClientRequest, OAuthJSONAccessTokenResponse.class);
            String accessToken = tokenResponse.getAccessToken();
            String tokenType = tokenResponse.getTokenType();
            Long expiresIn = tokenResponse.getExpiresIn();*/
            HttpPost httpPost = new HttpPost(TOKEN_URL);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("code", code));
            nvps.add(new BasicNameValuePair("client_id", CLIENT_ID));
            nvps.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
            nvps.add(new BasicNameValuePair("redirect_uri", redirectURI));
            nvps.add(new BasicNameValuePair("grant_type", GrantType.AUTHORIZATION_CODE.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.DEF_CONTENT_CHARSET));

            HttpClient httpClient = HttpClients.createDefault();
            HttpResponse httpPostResponse = null;
            try {
                httpPostResponse = httpClient.execute(httpPost);
                System.out.println(httpPostResponse.getEntity().getContent());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //System.out.println(accessToken + ":" + tokenType + ":" + expiresIn);
            //redirectURI += "#state=" + state + "&access_token=" + accessToken + "&token_type=" + tokenType;
            return new ModelAndView(new RedirectView(redirectURI));
        } catch (OAuthProblemException e) {
            e.printStackTrace();
        //} catch (OAuthSystemException e) {
        //    e.printStackTrace();
        }
        return new ModelAndView(new RedirectView("/"));
    }

    /*
     * Method that configures the spring application
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AlexaToDiscord.class);
    }

    /*
     * Method that allows us to map our Speechlet that handles the Alexa requests to the url /alexa
     */
    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        return new ServletRegistrationBean(createServlet(new DiscordSpeechlet()), "/alexa");
    }

    /*
     * Method that wraps a Speechlet in a SpeechletServlet which we can publish using spring
     */
    private SpeechletServlet createServlet(final Speechlet speechlet) {
        SpeechletServlet servlet = new SpeechletServlet();
        servlet.setSpeechlet(speechlet);
        return servlet;
    }

    private String findCookieValue(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        return "";
    }
}
