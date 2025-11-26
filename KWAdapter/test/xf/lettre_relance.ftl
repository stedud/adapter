<#ftl output_format="XML">
<?xml version="1.0"?>
<document>
  <parameter name="DOCTYPE" value="${_docType}"/>
   <parameter name="JOB_ID" value="${jobId}"/>
   <parameter name="DOC_POSITION" value="${_docNum!}"/>
   <parameter name="OUT_FILE_NAME" value="${_date!}-${_time!}"/>
   <parameter name="JOB_DATE" value="${_date!}"/>
   <parameter name="JOB_TIME" value="${_time!}"/>
   <parameter name="PRINTER_NAME" value="\\localhost\dummyPrinter"/>
   
   
  
  
  <target medium="LETTRE_RELANCE_PDF" pages="1-" number="01" type="orig">
  </target>
  <pagedefinitions/>
</document>