/**
 * Copyright (C) 2018-2021 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 * Idea by: Sergey Yaskov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.as2demo.springboot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.helger.as2servlet.AS2ReceiveServlet;
import com.helger.as2servlet.AbstractAS2ReceiveXServletHandler;
import com.helger.as2servlet.mdn.AS2MDNReceiveServlet;

@Configuration
public class ServletConfig
{
  @Bean
  public ServletRegistrationBean <AS2ReceiveServlet> servletRegistrationBeanAS2 ()
  {
    final ServletRegistrationBean <AS2ReceiveServlet> bean = new ServletRegistrationBean <> (new AS2ReceiveServlet (), "/as2");
    final Map <String, String> aInitParams = new HashMap <> ();
    aInitParams.put (AbstractAS2ReceiveXServletHandler.SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME, "config/config.xml");
    bean.setInitParameters (aInitParams);
    return bean;
  }

  @Bean
  public ServletRegistrationBean <AS2MDNReceiveServlet> servletRegistrationBeanMDN ()
  {
    final ServletRegistrationBean <AS2MDNReceiveServlet> bean = new ServletRegistrationBean <> (new AS2MDNReceiveServlet (), "/as2mdn");
    final Map <String, String> aInitParams = new HashMap <> ();
    aInitParams.put (AbstractAS2ReceiveXServletHandler.SERVLET_INIT_PARAM_AS2_SERVLET_CONFIG_FILENAME, "config/config.xml");
    bean.setInitParameters (aInitParams);
    return bean;
  }
}
