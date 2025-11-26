CodePage UTF8
StreamIn "horaire"
Begin


	Typeprefix;
	Record "ITEM" 2 ChrSep ";"
		NewEvent "HoraireBW";
		Match SCRIPT {
			if (matricule == null || matricule == undefined)
				{
					var matricule = "";
				}

			function check(aMatricule)
			{
				if (aMatricule != matricule)
				{
					matricule = aMatricule;
					return true;
				}
				else
					return false;
			}

			var fields = line.split(";");
			var thisMatricule = "";
			if (fields.length > 2) thisMatricule = fields[1];
			
			
			check(thisMatricule);
		}
		
		Fields
			
			"TypeEnregistrement"
			"Matricule"
			"Nom"
			"Pole"
			"CodeUnite"
			"Libelle"
			"CodeCout"
			"LibelleCout"
			"Mail"
			"Programme"
			"IndicateurConfiance"
			"DateCompteur"
			"DescriptionCompteur"
			"HC"
			"HDIV"
			"HS"
			"HeuresDispo"
			"NombreIndem"
			"IndemMontantUnitaire"
			"IndemMontantTotal"
			"HSdispo"
			"VC"
			"HeuresDues"
			"NouveauSoldeHC"
			"NouveausoldeHS" 
			"Ecretage"
			"NouveauSoldeVC"
			"SoldeHCPrecedent"
			"SoldeHSPrecedent"
			"SoldeVCprecedent"
			"NouveauSoldeHCTheorique"
			"SoldeHeuresNuit"
		End	
	End
	
	Typeprefix;
	Record "ITEM" 2 ChrSep ";"
		InEvent "HoraireBW";
		Match SCRIPT {
		
			function checkFields(fieldsArr)
			{
				if (fieldsArr == null || fieldsArr == undefined) return false;
				return fieldsArr.length > 1;
				
			}
			var fields = line.split(";");
			
			checkFields(fields);
		}
		Fields
			
			"TypeEnregistrement"
			"Matricule"
			"Nom"
			"Pole"
			"CodeUnite"
			"Libelle"
			"CodeCout"
			"LibelleCout"
			"Mail"
			"Programme"
			"IndicateurConfiance"
			"DateCompteur"
			"DescriptionCompteur"
			"HC"
			"HDIV"
			"HS"
			"HeuresDispo"
			"NombreIndem"
			"IndemMontantUnitaire"
			"IndemMontantTotal"
			"HSdispo"
			"VC"
			"HeuresDues"
			"NouveauSoldeHC"
			"NouveausoldeHS" 
			"Ecretage"
			"NouveauSoldeVC"
			"SoldeHCPrecedent"
			"SoldeHSPrecedent"
			"SoldeVCprecedent"
			"NouveauSoldeHCTheorique"
			"SoldeHeuresNuit"
		End	
	End	
	
End


