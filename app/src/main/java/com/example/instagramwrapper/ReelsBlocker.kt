package com.example.instagramwrapper

import android.webkit.WebView

object ReelsBlocker {
    fun script(blockReels: Boolean): String = """
        (function() {
          try {
            window.__instagramWrapperBlockReelsEnabled = ${if (blockReels) "true" else "false"};
            if (!window.__instagramWrapperBlockReelsEnabled && !window.__instagramWrapperReelsBlockerInstalled) {
              return;
            }
            if (window.__instagramWrapperReelsBlockerInstalled) {
              if (!window.__instagramWrapperBlockReelsEnabled && document && document.querySelectorAll) {
                var blockedNodes = document.querySelectorAll('[data-instagram-wrapper-blocked="reels"]');
                for (var blockedIndex = 0; blockedIndex < blockedNodes.length; blockedIndex++) {
                  var blockedNode = blockedNodes[blockedIndex];
                  blockedNode.style.pointerEvents = '';
                  blockedNode.style.display = '';
                  blockedNode.removeAttribute('aria-hidden');
                  delete blockedNode.dataset.instagramWrapperBlocked;
                }
              }
              return;
            }
            if (!window.__instagramWrapperBlockReelsEnabled) {
              return;
            }
            window.__instagramWrapperReelsBlockerInstalled = true;

            function parseUrl(rawUrl) {
              try {
                return new URL(rawUrl, window.location.href);
              } catch (error) {
                return null;
              }
            }

            function normalizeSegments(url) {
              try {
                return url.pathname
                  .split('/')
                  .filter(function(segment) { return segment && segment.length > 0; })
                  .map(function(segment) { return segment.toLowerCase(); });
              } catch (error) {
                return [];
              }
            }

            function isInstagramHost(host) {
              if (!host) {
                return false;
              }
              var normalizedHost = String(host).toLowerCase();
              return normalizedHost === 'instagram.com' || normalizedHost.endsWith('.instagram.com');
            }

            function isReelsUrl(url) {
              if (!window.__instagramWrapperBlockReelsEnabled) {
                return false;
              }
              if (!url || !isInstagramHost(url.host)) {
                return false;
              }
              var segments = normalizeSegments(url);
              return segments.length > 0 && segments[0] === 'reels';
            }

            function hideIfReelsLink(element) {
              if (!element || !element.getAttribute) {
                return;
              }
              var href = element.getAttribute('href') || element.href;
              if (!href) {
                return;
              }
              var parsed = parseUrl(href);
              if (!parsed || !isReelsUrl(parsed)) {
                return;
              }
              try {
                element.setAttribute('aria-hidden', 'true');
                element.style.pointerEvents = 'none';
                element.style.display = 'none';
                element.dataset.instagramWrapperBlocked = 'reels';
              } catch (error) {
              }
            }

            function scanElement(root) {
              if (!root || !root.querySelectorAll) {
                hideIfReelsLink(root);
                return;
              }
              hideIfReelsLink(root);
              var anchors = root.querySelectorAll('a[href]');
              for (var index = 0; index < anchors.length; index++) {
                hideIfReelsLink(anchors[index]);
              }
            }

            function stopIfReelsClick(event) {
              var target = event.target;
              while (target && target !== document.documentElement) {
                if (target.tagName === 'A' && target.href) {
                  var parsed = parseUrl(target.href);
                  if (parsed && isReelsUrl(parsed)) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (event.stopImmediatePropagation) {
                      event.stopImmediatePropagation();
                    }
                    return false;
                  }
                }
                target = target.parentElement;
              }
              return true;
            }

            function wrapHistoryMethod(methodName) {
              var original = history[methodName];
              if (typeof original !== 'function') {
                return;
              }
              history[methodName] = function() {
                try {
                  if (arguments.length > 2 && typeof arguments[2] === 'string') {
                    var parsed = parseUrl(arguments[2]);
                    if (parsed && isReelsUrl(parsed)) {
                      return null;
                    }
                  }
                } catch (error) {
                }
                var result = original.apply(this, arguments);
                scanElement(document.documentElement);
                return result;
              };
            }

            document.addEventListener('click', stopIfReelsClick, true);

            var observer = new MutationObserver(function(mutations) {
              for (var mutationIndex = 0; mutationIndex < mutations.length; mutationIndex++) {
                var mutation = mutations[mutationIndex];
                if (!mutation.addedNodes) {
                  continue;
                }
                for (var nodeIndex = 0; nodeIndex < mutation.addedNodes.length; nodeIndex++) {
                  scanElement(mutation.addedNodes[nodeIndex]);
                }
              }
            });

            if (document.documentElement) {
              observer.observe(document.documentElement, {
                childList: true,
                subtree: true
              });
              scanElement(document.documentElement);
            }

            wrapHistoryMethod('pushState');
            wrapHistoryMethod('replaceState');

            window.addEventListener('popstate', function() {
              scanElement(document.documentElement);
            }, true);
          } catch (error) {
          }
        })();
    """.trimIndent()

    fun inject(webView: WebView, blockReels: Boolean) {
        webView.evaluateJavascript(script(blockReels), null)
    }
}
