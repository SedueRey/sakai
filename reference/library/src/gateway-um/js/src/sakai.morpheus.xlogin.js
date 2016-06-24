
$PBJQ(document).ready(function(){

  $PBJQ("details.Mrphs-footer--details__build-info", "#footer").attr('open', '');

  $PBJQ('.Mrphs-portalWrapper').css('min-height', $PBJQ(window).innerHeight(true) );

  if( $PBJQ('#loginLink2').length == 1 ){

    $PBJQ('#loginLink2').click( function( e ){

      $PBJQ('body').append('<div id="Mrphs-xlogin-container" />');
      $PBJQ('#Mrphs-xlogin-container').load('/portal/xlogin #Mrphs-xlogin',function(){
        $PBJQ('#Mrphs-xlogin-container').addClass('loaded');
        $PBJQ('#Mrphs-xlogin').addClass('loadedByAjax');
        $PBJQ('#eid').focus();
      });
      $PBJQ('.Mrphs-portalWrapper').addClass('blurry');

      $PBJQ('body').append('<div id="loginPortalMask" />');
      $PBJQ('#loginPortalMask').bgiframe();
      
      $PBJQ('#loginPortalMask').click(function(){
        $PBJQ('#loginPortalMask').remove();
        $PBJQ('#Mrphs-xlogin-container').remove();
        $PBJQ('.Mrphs-portalWrapper').removeClass('blurry');
      });

      e.preventDefault();

    });
  }

});

$(window).scroll(function(){
  if( $(document).scrollTop() > 144 ){
    $('#toolMenu').addClass('scrolled');
  } else {
    $('#toolMenu').removeClass('scrolled');
  }
});