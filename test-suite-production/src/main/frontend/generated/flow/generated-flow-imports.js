import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'b29b80d92c67acc2064cd38c57bfecc8c74438801e47c519090029664a35cf1e') {
    pending.push(import('./chunks/chunk-aa97578cc570cb6231336b72aff17bf2d393901119382596c42d8a1bbb86e168.js'));
  }
  if (key === 'ab49dcd82083715c8689f0cfe9569fda4c5a93d660504827c3772803b69e4464') {
    pending.push(import('./chunks/chunk-b6689118629b0191588c41807d6e96903920a52dd45d425d6a682f48dcdb0736.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}