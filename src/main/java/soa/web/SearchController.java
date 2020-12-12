package soa.web;

import static soa.eip.Router.DIRECT_URI;


import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class SearchController {

  private final ProducerTemplate producerTemplateA;
  private final ProducerTemplate producerTemplateB;

  @Autowired
  public SearchController(ProducerTemplate producerTemplateA, ProducerTemplate producerTemplateB) {
    this.producerTemplateA = producerTemplateA;
    this.producerTemplateB = producerTemplateB;

  }

  @RequestMapping("/")
  public String index() {
    return "index";
  }


  @RequestMapping(value = "/search")
  @ResponseBody
  public Object search(@RequestParam("q") String q) {
    return producerTemplateB.requestBody("direct:filter", producerTemplateA.requestBody(DIRECT_URI, q));
  }
}