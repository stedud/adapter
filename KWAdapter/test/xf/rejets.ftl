<#ftl output_format="XML">
<?xml version="1.0"?>

<document>
   <parameter name="DOCTYPE" value="${_docType}"/>
   <parameter name="JOB_ID" value="${jobId}"/>
   <parameter name="DOC_POSITION" value="${_docNum!}"/>
   <parameter name="OUT_FILE_NAME" value="${destination!}"/>
   <parameter name="JOB_DATE" value="${_date!}"/>
   <parameter name="JOB_TIME" value="${_time!}"/>
   <parameter name="PRINTER_NAME" value=""/>
   <parameter name="CLIENT_NO" value="${client}"/>
	
   <parameter name="QR" value="${QR!}"/>

  
  <target medium="REJETS" pages="1-" number="01" type="orig">
  </target>
 
	<pagedefinitions>

	<pagedefinitions>
		<page number="1">
			 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			 <ocl value="PAGE_CONTROL PAPER_SOURCE=TRY01" />
		</page>
		<page number="3">
			 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			 <ocl value="PAGE_CONTROL PAPER_SOURCE=TRY02" />
		</page>
	</pagedefinitions>
	</pagedefinitions>
	
 </document>