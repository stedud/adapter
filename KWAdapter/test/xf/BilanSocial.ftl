<#ftl output_format="XML">
<?xml version="1.0"?>
<document>
	<parameter name="DOCTYPE" value="${_docType}"/>
		<parameter name="JOB_ID" value="${jobId}"/>
		<parameter name="DOC_POSITION" value="${_docNum!}"/>
		<parameter name="JOB_DATE" value="${_date!}"/>
		<parameter name="JOB_TIME" value="${_time!}"/>
		<parameter name="PRINTER_NAME" value=""/>
		<parameter name="DOCUMENT_NO" value="${Matricule_Nom}"/>
		<parameter name="EMAIL_TO" value="${Email}"/>
		<Parameter name="SIMPLEX_DUPLEX" value="DUPLEX"/>
		
		<#if Indicateur_Email=="X">
			<parameter name="INDICATEUR_EMAIL" value="true"/>
		<#else>
			<parameter name="INDICATEUR_EMAIL" value="false"/>
			<target medium="BILAN_SOCIAL_PAPIER" pages="1-" number="01" type="orig"></target>
		</#if>

		<target medium="BILAN_SOCIAL" pages="1-" number="01" type="orig">
		</target>
	<pagedefinitions>
	<page number="1">
	<ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" /> </page>
	</pagedefinitions>
</document>