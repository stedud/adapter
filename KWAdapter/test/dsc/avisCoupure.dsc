CodePage UTF8
StreamIn "InFile"

CHARSET "UTF-8"

Begin

	Record "AVIS" 1 ChrSep ";"
		Match SCRIPT {
			if (lineCounter == null || lineCounter == undefined)
				{
					var lineCounter = 0;
					//var callCounter = 0;
				}
				
			function check(aLine)
			{
				var matchAvis = aLine.startsWith('AVIS');
				if (! matchAvis)
				{
					return false;
				}
				
				
				
							
				if ((lineCounter % 2) == 0)
				{
					lineCounter = 1;
					return true;
					
				}
				else
				{
					return false;
					
				}
			}
			
			check(line);
		}
		NewEvent "AvisCoupure";
		Fields
			"ID";
			"Adr_Coupure";
			"Adr_Client_1";
			"Adr_Client_2";
			"Adr_Client_3";
			"Adr_Client_4";
			"Adr_Client_5";
			"Date_Debut1";
			"Date_Fin1";
			"Heure_Debut1";
			"Heure_Fin1";
			"Jour_Debut1";
			"Jour_Fin1";
			"Date_Debut2";
			"Date_Fin2";
			"Heure_Debut2";
			"Heure_Fin2";
			"Jour_Debut2";
			"Jour_Fin2";
			"Date_Debut3";
			"Date_Fin3";
			"Heure_Debut3";
			"Heure_Fin3";
			"Jour_Debut3";
			"Jour_Fin3";
			"Date_Debut4";
			"Date_Fin4";
			"Heure_Debut4";
			"Heure_Fin4";
			"Jour_Debut4";
			"Jour_Fin4";
			"Type_Travaux";
			"No_Client";
			"Statut";
			"No_Dossier";
		End
	End

	Record "AVIS" 1 ChrSep ";"
		Match SCRIPT{
			
			function check(aLine)
			{
				var matchAvis = aLine.startsWith('AVIS');
				if (! matchAvis)
				{
					return false;
					
				}
				lineCounter++;
				return true;
			}
			check(line);			
		}
		InEvent "AvisCoupure";
		Fields
			"ID";
			"Adr_Coupure";
			"Adr_Client_1";
			"Adr_Client_2";
			"Adr_Client_3";
			"Adr_Client_4";
			"Adr_Client_5";
			"Date_Debut1";
			"Date_Fin1";
			"Heure_Debut1";
			"Heure_Fin1";
			"Jour_Debut1";
			"Jour_Fin1";
			"Date_Debut2";
			"Date_Fin2";
			"Heure_Debut2";
			"Heure_Fin2";
			"Jour_Debut2";
			"Jour_Fin2";
			"Date_Debut3";
			"Date_Fin3";
			"Heure_Debut3";
			"Heure_Fin3";
			"Jour_Debut3";
			"Jour_Fin3";
			"Date_Debut4";
			"Date_Fin4";
			"Heure_Debut4";
			"Heure_Fin4";
			"Jour_Debut4";
			"Jour_Fin4";
			"Type_Travaux";
			"No_Client";
			"Statut";
			"No_Dossier";
		End
	End


End
