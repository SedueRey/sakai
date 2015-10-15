/*
	If there's too many sites on siteNav for sreen size we should organize them a little.
*/
/*var heightCalc = function(){
	var height = 0;
	if( !$PBJQ('body').hasClass( 'touch' ) ){
		height = ( $PBJQ("#Mrphs-sites-nav").height() > height )?$PBJQ("#Mrphs-sites-nav").height():height;
		height = ( $PBJQ("#toolMenuWrap").height() > height )?$PBJQ("#Mrphs-sites-nav").height():height;
		height = ( $PBJQ("#content").height() > height )?$PBJQ("#Mrphs-sites-nav").height():height;

		$PBJQ("#Mrphs-sites-nav").css( 'height', height + 'px' );
		$PBJQ("#toolMenuWrap").css( 'height', height + 'px' );
		$PBJQ("#content").css( 'height', height + 'px' );
	}else{
		$PBJQ("#Mrphs-sites-nav").removeAttr('style');
		$PBJQ("#toolMenuWrap").removeAttr('style');
		$PBJQ("#content").removeAttr('style');
	}
};

$PBJQ(window).load(function(){
	heightCalc();
	$PBJQ( window ).resize(function() {
		heightCalc();
	});
});*/

$PBJQ( document ).ready( function(){
	if( $PBJQ('#subSites').length > 0 ){
		$PBJQ('#subSites').hide();
		var subsites = '<li class="Mrphs-toolsNav__menuitem--sites">' + 
	                    '<a class="Mrphs-toolsNav__menuitem--link Mrphs-toolsNav__menuitem--subsites" accesskey="99" href="#" title="Asignaturas">' + 
	                    '    <span class="Mrphs-toolsNav__menuitem--icon icon-sakai-sites"> </span>' + 
	                    '   <span class="Mrphs-toolsNav__menuitem--title">Asignaturas</span>' + 
	                    '</a>' + 
                		'</li>';
        $PBJQ('#toolMenu ul').append( subsites );
        $PBJQ('.Mrphs-toolsNav__menuitem--subsites', '#toolMenu').on("click", function( e ){
        	e.preventDefault();
			$PBJQ('#subSites').css('top', $PBJQ(this).position().top ).toggle();
			$PBJQ(this).parent().toggleClass('is-current');
        });
	}
});
