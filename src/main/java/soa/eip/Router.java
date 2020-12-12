package soa.eip;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.LinkedList;
import org.apache.camel.Predicate;
import java.util.ArrayList;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.processor.aggregate.GroupedBodyAggregationStrategy;
import org.apache.camel.ExchangePattern;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class Router extends RouteBuilder {

  public static final String DIRECT_URI = "direct:twitter";

  private List<String> excludedTerms = new LinkedList<String>();

  @Override
  public void configure() {

    from(DIRECT_URI)
      .log("Body contains \"${body}\"")
      .process(new MyProcessor(excludedTerms))
      .log("Searching twitter for \"${body}\"!")
      .toD("twitter-search:${body}");

    from("direct:filter")
    .split(body(), new GroupedBodyAggregationStrategy())  //Split messages for separate processing
      .filter(new Predicate() {
        //Only predicates that match this predicate will execute the next action
        public boolean matches(Exchange exchange) {
            final String body = exchange.getIn().getBody(String.class);
            String userAndTweet[] = extractUsernameAndTweet(body);
            boolean result = body==null;
            Pattern p;
            for(String term : excludedTerms){
              if(result) break; //If any term has been found already, leave
              p = Pattern.compile("("+term+")");  //Filter any messages that contain the excluded term (in regex)

              result |= p.matcher(userAndTweet[0]).find();  //check if username contains term
              result |= p.matcher(userAndTweet[1]).find();  //check if tweet body contains term
            }
            return result;
        }})
          .setBody(constant(null))  //Set to null filtered messages
      .end()  //end of the filter
    .end()  //messages get regrouped
    .log("Body now contains the response from twitter:\n${body}");
}

  /*Syntax of messages:
   * DATE (USERNAME) TWEET
   */
  private String[] extractUsernameAndTweet(String body){
    String result[] = new String[2];
    result[0] = body.substring(body.indexOf("("), body.indexOf(")"));
    result[1] = body.substring(body.indexOf(")"));
    return result;
  }
  
  //Private class that processes requests, extracting excluded terms and "max" operators
  private class MyProcessor implements Processor{

    List<String> excludedTerms;

    public MyProcessor(List<String> excludedTerms){
      this.excludedTerms = excludedTerms;
    }

    public void process(Exchange exchange) throws Exception {
      
      excludedTerms.clear();
      String message = exchange.getIn().getBody(String.class);
      String splitted[] = message.split(" "); //Split the message in tokens
      String max = null;
      for(int i = 0; i < splitted.length; i++){
        if(splitted[i].matches("max:[0-9]+")){  //Find "max" argument
          max = splitted[i].substring(4); //Extract number
          splitted[i] = ""; //Erase token from message
        }
        else if(splitted[i].charAt(0) == '-'){  //Find excluded terms
          excludedTerms.add(splitted[i].substring(1)); //Extract term
          splitted[i] = ""; //Erase token from message
        }
      }
      message = String.join(" ", splitted); //Join all the tokens in a single string
      if(max != null){
        message += "?count=" + max; //Add count argument for URI
      }
      exchange.getOut().setBody(message.trim()); //Set the body
    }
  }
}
