// Populate the sidebar
//
// This is a script, and not included directly in the page, to control the total size of the book.
// The TOC contains an entry for each page, so if each page includes a copy of the TOC,
// the total size of the page becomes O(n**2).
class MDBookSidebarScrollbox extends HTMLElement {
    constructor() {
        super();
    }
    connectedCallback() {
        this.innerHTML = '<ol class="chapter"><li class="chapter-item expanded "><a href="preface.html"><strong aria-hidden="true">1.</strong> Preface</a></li><li class="chapter-item expanded "><a href="chapter-01.html"><strong aria-hidden="true">2.</strong> Chapter 01 — Introduction</a></li><li class="chapter-item expanded "><a href="chapter-02.html"><strong aria-hidden="true">3.</strong> Chapter 02 — Java 17 vs Java 21 — An FP-Oriented Comparison</a></li><li class="chapter-item expanded "><a href="chapter-03.html"><strong aria-hidden="true">4.</strong> Chapter 03 — Option / Either / Try — Algebraic Data Types in a Nominal World</a></li><li class="chapter-item expanded "><a href="chapter-04.html"><strong aria-hidden="true">5.</strong> Chapter 04 — The State Monad</a></li><li class="chapter-item expanded "><a href="chapter-05.html"><strong aria-hidden="true">6.</strong> Chapter 05 — IO and Effect Suspension</a></li><li class="chapter-item expanded "><a href="chapter-06.html"><strong aria-hidden="true">7.</strong> Chapter 06 — Functional Data Structures: List</a></li><li class="chapter-item expanded "><a href="chapter-07.html"><strong aria-hidden="true">8.</strong> Chapter 07 — Tree and Structural Recursion</a></li><li class="chapter-item expanded "><a href="chapter-08.html"><strong aria-hidden="true">9.</strong> Chapter 08 — Parser Combinator Basics in Java</a></li><li class="chapter-item expanded "><a href="chapter-09.html"><strong aria-hidden="true">10.</strong> Chapter 09 — Building a Minimal Parser Library</a></li><li class="chapter-item expanded "><a href="chapter-10.html"><strong aria-hidden="true">11.</strong> Chapter 10 — Free Monad Essentials</a></li><li class="chapter-item expanded "><a href="chapter-11.html"><strong aria-hidden="true">12.</strong> Chapter 11 — Designing an Expr DSL Using Free</a></li><li class="chapter-item expanded "><a href="chapter-12.html"><strong aria-hidden="true">13.</strong> Chapter 12 — Interpretation, Optimization, and Code Generation</a></li><li class="chapter-item expanded "><a href="chapter-13.html"><strong aria-hidden="true">14.</strong> Chapter 13 — Why FP Matters Beyond Syntax</a></li></ol>';
        // Set the current, active page, and reveal it if it's hidden
        let current_page = document.location.href.toString().split("#")[0].split("?")[0];
        if (current_page.endsWith("/")) {
            current_page += "index.html";
        }
        var links = Array.prototype.slice.call(this.querySelectorAll("a"));
        var l = links.length;
        for (var i = 0; i < l; ++i) {
            var link = links[i];
            var href = link.getAttribute("href");
            if (href && !href.startsWith("#") && !/^(?:[a-z+]+:)?\/\//.test(href)) {
                link.href = path_to_root + href;
            }
            // The "index" page is supposed to alias the first chapter in the book.
            if (link.href === current_page || (i === 0 && path_to_root === "" && current_page.endsWith("/index.html"))) {
                link.classList.add("active");
                var parent = link.parentElement;
                if (parent && parent.classList.contains("chapter-item")) {
                    parent.classList.add("expanded");
                }
                while (parent) {
                    if (parent.tagName === "LI" && parent.previousElementSibling) {
                        if (parent.previousElementSibling.classList.contains("chapter-item")) {
                            parent.previousElementSibling.classList.add("expanded");
                        }
                    }
                    parent = parent.parentElement;
                }
            }
        }
        // Track and set sidebar scroll position
        this.addEventListener('click', function(e) {
            if (e.target.tagName === 'A') {
                sessionStorage.setItem('sidebar-scroll', this.scrollTop);
            }
        }, { passive: true });
        var sidebarScrollTop = sessionStorage.getItem('sidebar-scroll');
        sessionStorage.removeItem('sidebar-scroll');
        if (sidebarScrollTop) {
            // preserve sidebar scroll position when navigating via links within sidebar
            this.scrollTop = sidebarScrollTop;
        } else {
            // scroll sidebar to current active section when navigating via "next/previous chapter" buttons
            var activeSection = document.querySelector('#sidebar .active');
            if (activeSection) {
                activeSection.scrollIntoView({ block: 'center' });
            }
        }
        // Toggle buttons
        var sidebarAnchorToggles = document.querySelectorAll('#sidebar a.toggle');
        function toggleSection(ev) {
            ev.currentTarget.parentElement.classList.toggle('expanded');
        }
        Array.from(sidebarAnchorToggles).forEach(function (el) {
            el.addEventListener('click', toggleSection);
        });
    }
}
window.customElements.define("mdbook-sidebar-scrollbox", MDBookSidebarScrollbox);
