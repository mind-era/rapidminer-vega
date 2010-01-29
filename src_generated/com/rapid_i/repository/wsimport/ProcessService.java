
package com.rapid_i.repository.wsimport;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.1 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebService(name = "ProcessService", targetNamespace = "http://service.web.rapidrepository.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface ProcessService {


    /**
     * 
     * @param arg0
     * @return
     *     returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "stopProcess", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.StopProcess")
    @ResponseWrapper(localName = "stopProcessResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.StopProcessResponse")
    public boolean stopProcess(
        @WebParam(name = "arg0", targetNamespace = "")
        int arg0);

    /**
     * 
     * @param processLocation
     * @param executionTime
     * @return
     *     returns com.rapid_i.repository.wsimport.ExecutionResponse
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "executeProcessSimple", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.ExecuteProcessSimple")
    @ResponseWrapper(localName = "executeProcessSimpleResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.ExecuteProcessSimpleResponse")
    public ExecutionResponse executeProcessSimple(
        @WebParam(name = "processLocation", targetNamespace = "")
        String processLocation,
        @WebParam(name = "executionTime", targetNamespace = "")
        XMLGregorianCalendar executionTime);

    /**
     * 
     * @param processLocation
     * @param start
     * @param cronExpression
     * @param end
     * @return
     *     returns com.rapid_i.repository.wsimport.ExecutionResponse
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "executeProcessCron", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.ExecuteProcessCron")
    @ResponseWrapper(localName = "executeProcessCronResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.ExecuteProcessCronResponse")
    public ExecutionResponse executeProcessCron(
        @WebParam(name = "processLocation", targetNamespace = "")
        String processLocation,
        @WebParam(name = "cronExpression", targetNamespace = "")
        String cronExpression,
        @WebParam(name = "start", targetNamespace = "")
        XMLGregorianCalendar start,
        @WebParam(name = "end", targetNamespace = "")
        XMLGregorianCalendar end);

    /**
     * 
     * @param triggerName
     * @return
     *     returns com.rapid_i.repository.wsimport.Response
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "cancelTrigger", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.CancelTrigger")
    @ResponseWrapper(localName = "cancelTriggerResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.CancelTriggerResponse")
    public Response cancelTrigger(
        @WebParam(name = "triggerName", targetNamespace = "")
        String triggerName);

    /**
     * 
     * @return
     *     returns java.util.List<java.lang.Integer>
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getRunningProcesses", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.GetRunningProcesses")
    @ResponseWrapper(localName = "getRunningProcessesResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.GetRunningProcessesResponse")
    public List<Integer> getRunningProcesses();

    /**
     * 
     * @param arg0
     * @return
     *     returns com.rapid_i.repository.wsimport.ProcessResponse
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getRunningProcessesInfo", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.GetRunningProcessesInfo")
    @ResponseWrapper(localName = "getRunningProcessesInfoResponse", targetNamespace = "http://service.web.rapidrepository.com/", className = "com.rapid_i.repository.wsimport.GetRunningProcessesInfoResponse")
    public ProcessResponse getRunningProcessesInfo(
        @WebParam(name = "arg0", targetNamespace = "")
        int arg0);

}