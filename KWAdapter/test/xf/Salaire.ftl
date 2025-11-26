<#ftl output_format="XML">
<?xml version="1.0"?>
<document>
	<parameter name="DOCTYPE" value="${_docType}"/>
		<parameter name="JOB_ID" value="${jobId}"/>
		<parameter name="DOC_POSITION" value="${_docNum!}"/>
		<parameter name="OUT_FILE_NAME" value="-"/>
		<parameter name="JOB_DATE" value="${_date!}"/>
		<parameter name="JOB_TIME" value="${_time!}"/>
		<parameter name="PRINTER_NAME" value=""/>
		<parameter name="DOCUMENT_NO" value="${No_Matricule}"/>
		<parameter name="EMAIL_TO" value="${Email}"/>
		<!-- <parameter name="EMAIL_TO" value="${Email}"/>-->
		<parameter name="CLIENT_NO" value="${No_Matricule}"/>
		<parameter name="PERIODE_SALAIRE" value="${Periode!}"/>

  
		<target medium="SALAIRE_ARCHIVE" pages="1-" number="01" type="orig">
		</target>
		<#if Email?has_content>
			<target medium="SALAIRE_MAIL" pages="1-" number="01" type="orig">
			</target>
		<#else>
			<target medium="SALAIRE_PRINT" pages="1-" number="01" type="orig">
			</target>
		</#if>
	<pagedefinitions>
	<page number="1">
	<ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" /> </page>
	</pagedefinitions>
</document>