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
   <parameter name="DOCUMENT_NO" value="${Facture_No}"/>
   <parameter name="CLIENT_NO" value="${Client_No}"/>
   <#if Echeance?has_content>
	   <parameter name="ECHEANCE" value="${(Echeance?datetime("dd.MM.yyyy"))?string["yyyyMMdd"]}"/>
   <#else>
   	<parameter name="ECHEANCE" value=""/>
   	</#if>
   <parameter name="COMPTE_NO" value="${Compte}"/>
   <parameter name="FACTURE_NO" value="${Facture_No}"/>
   <parameter name="MONTANT" value="${Montant}"/>
   <parameter name="EMAIL_FROM" value="${From}"/>
   <parameter name="EMAIL_TO" value="${To}"/>
   <parameter name="NIVEAU_RELANCE" value="${Niveau}"/>

  
  <target medium="RELANCE_MAIL" pages="1-" number="01" type="orig">
  </target>
  <pagedefinitions/>
</document>