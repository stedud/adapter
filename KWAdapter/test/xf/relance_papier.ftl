<#ftl output_format="XML">
<?xml version="1.0"?>

<#assign type_out=type[5..*1]>
<#switch type_out>
  <#case "X">
	<#assign pliage="1">
	<#assign destination="RELANCE_PAPIER_CONDITIONNEMENT">
  <#break>

  <#case "_">
	<#assign pliage="0">
	<#assign destination="RELANCE_PAPIER_SIGNATURE">
  <#break>

  <#case "E">
  	<#assign pliage="E">
  	<#assign destination="RELANCE_PAPIER_ETRANGER">
  <#break>
</#switch>

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

  
  <target medium="${destination}" pages="1-" number="01" type="orig">
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