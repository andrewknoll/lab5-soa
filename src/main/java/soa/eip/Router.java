package soa.eip;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class Router extends RouteBuilder {

  public static final String DIRECT_URI = "direct:twitter";

  @Override
  public void configure() {

    from(DIRECT_URI)
      .log("Body contains \"${body}\"")
      .log("Searching twitter for \"${body}\"!")
      .process(new MyProcessor())
      .toD("twitter-search:${body}")
      .log("Body now contains the response from twitter:\n${body}");
  }
  private class MyProcessor implements Processor{
    public void process(Exchange exchange) throws Exception {
      
      String message = exchange.getIn().getBody(String.class);
      String splitted[] = message.split(" ");
      String max = "";
      for(int i = 0; i < splitted.length; i++){
        if(splitted[i].matches("max:[0-9]+")){
          max = splitted[i].substring(4);
          splitted[i] = "";
        }
      }
      message = String.join(" ", splitted);
      if(!max.equals("")){
        message += "?count=" + max;
      }
      exchange.getOut().setBody(message);
    }
  }
}
