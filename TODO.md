# CSCI415PROJECT
A joint project between the DNS and HTTP Groups.
- DNS: A basic server that handles type-A DNS request. Must use java datagram socket for networking library. 
  - Must keep a small database of IP addresses.
  - Be able to parse DNS request message and send fake IP address back to client for certain domains.
  - Ignore all other types of DNS request messages.   
- HTTP: A web server that supports IP address filtering and direct traffic to fake websites.
  - Be able to parse HTTP Get request messages.
  - Direct the traffic to different pages based on the host domain.
  - Be able to block HTTP request from certain IP address (Always send 404 file not found response back to this IP address).
  - Support HTTP response message with status codes (200, 400, and 404).
  - Support text/html, imag/jpg, and application/pdf.
  - Main html index file.
  - (Optional) Support Mutlithreading.

### TODO
- [ ] First task #Group(DNS/HTTP) @contributor dd-mm-yy
  - [ ] Sub-task/description

### Completed Column ✓
- [x] Completed task title 
