class HelloAddon extends HTMLElement {
    connectedCallback() {
        this.textContent = 'Hello from the add-on';
    }
}
customElements.define('hello-addon', HelloAddon);
