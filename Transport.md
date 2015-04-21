# org.apache.commons.net.ssh.transport #

Transport layer of the SSH protocol

## State diagram ##

1. Initial state.

2. `Transport#init` has been called; key exchange / algorithm negotiation commences. `init` blocks until the `KEX_DONE` state has been reached. We delegate handling of incoming packets to `Negotiator#handle`, and when it returns `true` we take it to mean that key exchange has been completed.

3. In this state, a service request may be made via `Transport#reqService`, e.g. for `ssh-userauth`. We request the service, and transition to state `SERVICE_REQ` where we await acceptance notificaiton.

4. Waiting for acceptance notification.

5. We have received `SSH_MSG_SERVICE_ACCEPT`. We can delegate packet handling to the `Service` instance --- except when we receive `SSH_MSG_KEXINIT` from server, which indicates re-exchange should start; we prevent any other packets through while re-exchanging. Again, when `Negotiator#handle returns `true`, we know it is done and are back in `SERVICE`.

6. `Transport#disconnect` results in ending up in `DEAD`

![http://lh6.ggpht.com/_E6UWPPTmHCk/Slt8S7wzcnI/AAAAAAAAACM/ILy-1EOA89I/s800/transport_state_diagram.png](http://lh6.ggpht.com/_E6UWPPTmHCk/Slt8S7wzcnI/AAAAAAAAACM/ILy-1EOA89I/s800/transport_state_diagram.png)