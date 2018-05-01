package ${dto.interface.packageName};

#set ($ignore = $dto.interface.imports.add("com.ngc.seaside.service.fault.api.ServiceFaultException"))
#foreach ($i in $dto.interface.imports)
import ${i};
#end

public interface ${dto.interface.name} {

################################## Pub sub methods ###################################
#foreach ($method in $dto.basicPubSubMethods)
   ${method.output.type} ${method.serviceMethod}(${method.input.type} ${method.input.fieldName}) throws ServiceFaultException;

#end
################################## Sink methods ###################################
#foreach ($method in $dto.basicSinkMethods)
   void ${method.serviceMethod}(${method.input.type} ${method.input.fieldName}) throws ServiceFaultException;

#end
################################## Correlation methods ###################################
#foreach ($method in $dto.correlationMethods)
   ${method.output.type} ${method.serviceMethod}(
#foreach ($input in $method.inputs)
      ${input.type} ${input.fieldName},
#end
      ILocalCorrelationEvent<${method.correlationType}> correlationEvent) throws ServiceFaultException;

#end
################################## Req/res Methods ###################################
#foreach ($method in $dto.basicServerReqResMethods)
   ${method.output.type} ${method.serviceMethod}(${method.input.type} ${method.input.fieldName}) throws ServiceFaultException;

#end
}
