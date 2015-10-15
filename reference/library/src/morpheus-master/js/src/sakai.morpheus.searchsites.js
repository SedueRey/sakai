
$PBJQ(document).ready(function(){

	if( $PBJQ('#Mrphs-xlogin').length === 0 ){	

		$('body.Mrphs-portalBody').prepend('<div id="searchSites"><input id="qsites" value="" /><i class="fa fa-lg fa-search"></i></div>');
		$('#qsites').on('keyup', function(){
			var busqueda = $PBJQ('#qsites').val().trim().toLowerCase();
			if( $PBJQ("#qsites").val() !== "" ){
				$PBJQ(".Mrphs-sitesNav__menuitem span", "#Mrphs-sites-nav").not('.Mrphs-sitesNav__drop').each( function( i ){
					if( $(this).text() !== "" ){				
						var textoTratado = $PBJQ(this).text().trim().toLowerCase();
						if( textoTratado.indexOf( busqueda ) < 0 ){
							$PBJQ(this).parent().parent().slideUp();
						}else{
							$PBJQ(this).parent().parent().slideDown();
						}
					}
				});
			}else{
				console.log("el contenido del input está vacío");
				$PBJQ(".Mrphs-sitesNav__menuitem").slideDown();
			}
		});

	}
});