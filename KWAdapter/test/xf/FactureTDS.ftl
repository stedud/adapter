<#ftl output_format="XML">
<?xml version="1.0"?>
<document>
	<parameter name="DOCTYPE" value="${_docType!}"/>
	<parameter name="JOB_ID" value="${jobId!}"/>
	<parameter name="DOC_POSITION" value="${_docNum!}"/>
	<parameter name="JOB_DATE" value="${_date!}"/>
	<parameter name="JOB_TIME" value="${_time!}"/>
	<parameter name="PRINTER_NAME" value="${PJDEPARTMENT!}"/>
	<parameter name="SIMPLEX_DUPLEX" value="DUPLEX"/>
	
	<parameter name="PARTNER_ID" value="${PartnerID!}"/>
	<parameter name="COMPTE_NO" value="${CompteID!}"/>
	<parameter name="DATE_FACTURE" value="${Date_Facture!}"/>
	<parameter name="FACTURE_NO" value="${InvoiceID!}"/>
		
	<parameter name="RQID" value="${RQID}"/>
	<parameter name="RQCREATIME" value="${RQCREATIME}"/>
	<parameter name="RQOWNER" value="${RQOWNER}"/>
	
	<parameter name="QR" value="${QR!}"/>
	
	<parameter name="CLIENT_NO" value="${client_num!}"/>
	<parameter name="TYPE_COURRIER" value="${GV_TYPE_COURRIER!}"/>
	

	<#if Mail_Destinataire?has_content>
	<parameter name="EMAIL_TO" value="${Mail_Destinataire!}"/>
	</#if>

	<target medium="FACTURE_TDS_ARCHIVE" pages="1-" number="01" type="orig"></target>
	
	
	<#if PJDEPARTMENT != "KPDF">
		<#assign directPrinters = ["M007", "M021", "M032", "M033", "M054", "M026", "MFSD"]>
		<#if directPrinters?seq_contains(PJDEPARTMENT)>
			<target medium="FACTURE_TDS_DIRECT" pages="1-" number="01" type="orig">
			</target>
			
		<#else>
			
			<target medium="FACTURE_TDS" pages="1-" number="01" type="orig">
			</target>

			
		</#if>
	</#if>
	<pagedefinitions>
			<page number="1">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY01" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="2">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY01" />
			</page>
			<page number="3">
					<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="4">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="5">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="6">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="7">
					<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="8">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="9">
					<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="10">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="11">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>
			<page number="12">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="13">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="14">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="15">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="16">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="17">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="18">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="19">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="20">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="21">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="22">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="23">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="24">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="25">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="26">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="27">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="28">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="29">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
			<page number="30">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
			</page>
			<page number="31">
				<ocl value="PAGE_CONTROL PAPER_SOURCE=TRY04" />
				 <ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX" />
			</page>	
	</pagedefinitions>
</document>