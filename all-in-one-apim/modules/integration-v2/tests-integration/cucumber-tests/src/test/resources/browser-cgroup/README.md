The Linux SSO browser runs in a private network namespace with stable loopback and dummy interfaces. A filesystem
Unix socket connects its loopback proxy relay to the test JVM's CONNECT proxy. Chromium keeps the invoking user's
UID and Playwright's debugging socketpairs on descriptors 3/4. The optional cgroup covers the browser and its
namespace helpers with 1 CPU. All-in-one uses a 2.5 GiB memory limit and no swap; distributed CI uses a 512 MiB
memory limit with up to 128 MiB of swap. The distributed browser cgroup is part of the aggregate test resource budget.
Its cgroup is managed by the transient `apimdistributed-browser.service` beneath `apimdistributed.slice`.
An idle `sleep` process keeps the service alive and is included in the browser's budget. Systemd retains ownership
of the CPU and memory limits when other test scopes start; distributed cleanup stops the service and its browser
processes. The all-in-one browser uses the root `pw-browser` cgroup.

Linux setup (CI performs these steps):

```bash
sudo apt-get install -y socat iproute2 util-linux sudo
sudo bash src/test/resources/browser-cgroup/setup-browser-network.sh
sudo bash src/test/resources/browser-cgroup/setup-browser-cgroup.sh
```

Run the scripts from the `cucumber-tests` module. The network setup requires passwordless sudo for the browser
wrapper's `unshare` command and scopes the close-from override to that command; it writes the world-readable
marker `/etc/pw-browser-network` that `BrowserCgroup` checks before using the namespace wrapper. Linux hosts
without that setup, and macOS, launch Chromium directly. The cgroup setup is optional for local Linux runs, but
requires the network setup.
