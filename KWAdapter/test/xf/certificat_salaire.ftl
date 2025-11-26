<#ftl output_format="XML">
<?xml version="1.0"?>
	<document>
		<parameter name="DOCTYPE" value="${_docType}"/>
		<parameter name="JOB_ID" value="${jobId}"/>
		<parameter name="DOC_POSITION" value="${_docNum!}"/>
		<parameter name="OUT_FILE_NAME" value="${_date!}-${_time!}"/>
		<parameter name="JOB_DATE" value="${_date!}"/>
		<parameter name="JOB_TIME" value="${_time!}"/>
		<parameter name="PRINTER_NAME" value="dummyPrinter"/>
		<parameter name="CLIENT_NO" value="${matriculePDF!}"/>
		<parameter name="ANNEE" value="${AnneePDF!}"/>
		<#assign fullAddress="${LigneAddresse4!''},${LigneAddresse3!''},${LigneAddresse2!'N/A'}">
		<#assign COUNTRY="ET">
		<#list fullAddress?split(",") as x>
			<#if x?starts_with('CH-')>
				<#assign COUNTRY="CH">
			</#if>
		</#list>
				
		<parameter name="ADDRESS" value="${COUNTRY!''}"/>
		<target medium="CERTIFICAT_SALAIRE_PAPIER_${COUNTRY!'CH'}" pages="1-" number="01" type="orig">
			<pagedefinitions>
				<page number="1">
					<ocl value="PAGE_CONTROL DUPLEX_MODE=DUPLEX"/>
				</page>
			</pagedefinitions>
		</target>
		<target medium="CERTIFICAT_SALAIRE" pages="1-" number="01" type="orig">
		</target>
	</document>