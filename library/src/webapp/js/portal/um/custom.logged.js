// AV-15482
document.addEventListener("DOMContentLoaded", () => {
  const listadoModal = `
    <div class="bbCollaborate__modalListado" id="bbCollaborate__modalListado">
      <div class="bbCollaborate__modalListado_bg"></div>
      <div class="bbCollaborate__modalListado_inner">
        <span class="bbCollaborate__modalListado_close">x</span>
        <h1>Fin del soporte de Blackboard Collaborate</h1>
        <p>El servicio de videoconferencia BlackBoard ha dejado de estar disponible.</p>
        <p>Las videoconferencias de clase deberán ser planificadas y realizadas desde la opción de VIDEOCLASE.
        Para más información puede ponerse en contacto con nosotros llamando al
        Centro de Atención a Usuarios de ÁTICA (extensión 4222)</p>
      </div>
    </div>
  `;
  document.querySelectorAll('.Mrphs-toolsNav__menuitem--icon.icon-sakai--sakai-collaborate ').forEach(element => {
    const parent = element.parentElement;
    parent.addEventListener('click', function(e) {
      const urlTarget = this.getAttribute('href');
      e.preventDefault();

      document.body.insertAdjacentHTML('beforeend', listadoModal);

      document.querySelectorAll('.bbCollaborate__modalListado_close, .bbCollaborate__modalListado_bg').forEach(el => {
        el.addEventListener('click', function() {
          document.getElementById('bbCollaborate__modalListado').remove();
        });
      });

      const innerElement = document.getElementById('bbCollaborate__modalListado_inner');
      if (innerElement) {
        innerElement.addEventListener('click', function(evt) {
          evt.preventDefault();
          evt.stopPropagation();
        });
      }

      document.querySelectorAll('.bbCollaborate__modalListado_accept').forEach(el => {
        el.addEventListener('click', function() {
          document.location.href = urlTarget;
        });
      });

      return false;
    });
  });
});


// AV-20015
document.addEventListener("DOMContentLoaded", function(event) {
  document.querySelectorAll('iframe[src^="https://view.genial.ly/"]').forEach(e => { e.style.position = "absolute"; }); 
});

//AV-28097 - Estudiar posibilidades de herramienta de transición a teams
window.addEventListener('DOMContentLoaded', () => {
	const teamsSpan = document.getElementById('um-teams');
  if (!teamsSpan) return;
	const urlTeams = teamsSpan.getAttribute('rel');
	if (urlTeams !== '') {
		const script = document.createElement('script');
		script.id = 'avteams';
		script.src = urlTeams;
		script.async = true;
		script.onload = function() {
		if (window.AV_TEAMS) {
				try {
					const idSitio = window.location.pathname.split('/').findLast((el) => el).split('_')[0];
					if (idSitio) {
						if (document.body.classList.contains('delegaciones')) {
							const delegacionesArray = Object.values(AV_TEAMS.delegaciones);
							const delegacionTeam = delegacionesArray.find(
								(el) => el.centerCode === idSitio && el.populated === true
							);
							if (delegacionTeam) {
								document.getElementById('um-teams-url').setAttribute("href", delegacionTeam.link);
								$PBJQ('#um-teams-message').show();
							}
						}
					}
				} catch (error) {}
			}
		}
		document.body.append(script);
	}
});

// Búsqueda de sitios
window.addEventListener('DOMContentLoaded', () => {
  const headButton = document.getElementById('otherSiteSearchHeadButton')
  if (!headButton) return;
  headButton.addEventListener('click', function() {
    const searchInput = document.getElementById('search-all-sites');
    if (searchInput) {
      window.setTimeout(function () { 
        searchInput.focus({ focusVisible: true });
      }, 500); 
    }
  });
  const searchHead = document.getElementById('otherSiteSearchHead');
  if (!searchHead) return;
  searchHead.addEventListener('click', function() {
    document.getElementById('otherSiteSearchHeadButton').click();
  });
  const sidebarCollapsedButton = document.getElementById('sidebarCollapsedButton');
  if (!sidebarCollapsedButton) return;
  sidebarCollapsedButton.addEventListener('click', function() {
    document.getElementById('otherSiteSearchHeadButton').click();
  });

});

window.addEventListener('DOMContentLoaded', () => {
  const hamburgerButton = document.getElementById('sidebar-collapse-button');
  if (!hamburgerButton) return;
  const sidenav = document.getElementById('portal-nav-sidebar');
  if (!sidenav.classList.contains('sidebar-collapsed')) {
    document.getElementById('portal-nav-sidebar-alternate').classList.remove('sidebar-collapsed');
  }
  document.getElementById('portal-nav-sidebar-alternate').addEventListener('click', function() {
    document.getElementById('sidebar-collapse-button').click();
  });
  hamburgerButton.addEventListener('click', function() {
    window.setTimeout(function () { 
      document.getElementById('portal-nav-sidebar-alternate').classList.toggle('sidebar-collapsed');
    }, 120);
  });
});