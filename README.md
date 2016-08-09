# puff-puff-dash

## Prerequisites

+ Docker
+ docker-compose 1.6+ (for deployment)
+ Ruby 1.9.3+ (for Rake)
+ Leiningen 2.0

## Configuration

TODO

## Running

### Local Development

Since I am typically in tmux, the Rake task `dev:start` is configured to
split the current tmux window and run both the backend and frontend
services. If you are not using tmux for some reason or would like to
start only one of the services, the following commands will do that:

+ `lein run` - start the server
+ `lein figwheel` - start Figwheel for ClojureScript

The database server must also be started with `rake dev:db:start`.

### Docker

A `docker-compose.yml` file is provided that includes
configuration for both the database and web application containers.

A Rake task for starting the containers locally is not currently implemented.
To start them, all you should need to do is build the JAR with
`lein uberjar` and `docker-compose up`.

By default, port 3104 is mapped to the container port 3000. This will
probably be made configurable (along with a bunch more) in the future.
At this point, you should be able to point your browser to `http://localhost:3104`
and if everything went well, the page should load successfully.

## Deployment

Right now all deployment configuration is hardcoded, but eventually it will
probably be moved to `resources/config.yml`.

The following are options related to deployment, and are found in `$config[:deploy]`:

+ `db`
    + `container_name` - the database container name
    + `username` - the database user
    + `password` - the database password
+ `remote_path` - where the project files will be pushed
+ `remote_user` - the deploy user
+ `ssh_port` - the SSH port of the server
+ `host` - the hostname or IP address of the server

To deploy, simply run `rake prod:deploy`. This will do the following:

1. Run `lein uberjar` to build the JAR (unless `NO_BUILD` or `NO_SYNC` are set)
2. `rsync` necessary files (unless `NO_SYNC` is set):
    + `target/uberjar/*.jar`
    + `docker-compose.yml`
    + `Dockerfile`
    + `Dockerfile-db`
    + `Rakefile`
    + `resources/migrations/`
    + `resources/bin/`
3. Bring down current services if they are running
4. Build the images
5. Start the containers

After that, the services should be running and accessible internally via the
configured listen port (default 3104 above). Your webserver (Nginx in my case)
should then be given a virtual host to proxy requests, e.g.

    server {
      listen 80;
      server_name ppd.intern.xyzyxyzy.xyz;
      client_max_body_size 20m;
        
      access_log /var/log/nginx/ppd.intern.xyzyxyzy.xyz.access.log;
      error_log /var/log/nginx/ppd.intern.xyzyxyzy.xyz.error.log;
      
      location / {
        proxy_pass http://localhost:3104;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_redirect off;
      }

      root /www/intern.xyzyxyzy.xyz/static;
      index index.html;
    }

## API

### `/links`

<table>
  <tr>
    <th>Route</th>
    <th>Method</th>
    <th>Body</th>
    <th>URL parameters</th>
    <th>Route parameters</th>
    <th>Response</th>
  </tr>
  <tr>
    <td><code>/links</code></td>
    <td>GET</td>
    <td>Optional query JSON</td>
    <td></td>
    <td></td>
    <td>Links matching criteria</td>
  </tr>
  <tr>
    <td><code>/links/:source</code></td>
    <td>GET</td>
    <td></td>
    <td></td>
    <td><code>source</code></td>
    <td>Links from given <code>:source</code></td>
  </tr>
  <tr>
    <td><code>/links/:source</code></td>
    <td>POST</td>
    <td>Links JSON</td>
    <td>optional initial <code>tag</code></td>
    <td><code>source</code></td>
    <td>Total links imported or error</td>
  </tr>
  <tr>
    <td><code>/links/:id</code></td>
    <td>GET</td>
    <td></td>
    <td></td>
    <td><code>id</code></td>
    <td>Link by <code>:id</code></td>
  </tr>
  <tr>
    <td><code>/links/:id</code></td>
    <td>DELETE</td>
    <td></td>
    <td></td>
    <td><code>id</code></td>
    <td>Delete link <code>:id</code></td>
  </tr>
  <tr>
    <td><code>/links/:id/tags</code></td>
    <td>GET</td>
    <td></td>
    <td></td>
    <td><code>id</code></td>
    <td>Get links tagged with <code>:tag</code></td>
  </tr>
  <tr>
    <td><code>/links/:id/tags/:tag</code></td>
    <td>POST</td>
    <td></td>
    <td></td>
    <td><code>id tag</code></td>
    <td>Tag link</td>
  </tr>
  <tr>
    <td><code>/links/:id/tags/:tag</code></td>
    <td>DELETE</td>
    <td></td>
    <td></td>
    <td><code>id tag</code></td>
    <td>Remove tag from link</td>
  </tr>
</table>

## `/tags`

<table>
  <tr>
    <th>Route</th>
    <th>Method</th>
    <th>Body</th>
    <th>URL parameters</th>
    <th>Route parameters</th>
    <th>Response</th>
  </tr>
  <tr>
    <td><code>/tags</code></td>
    <td>GET</td>
    <td></td>
    <td></td>
    <td></td>
    <td>Number of links per tag</td>
  </tr>  
</table>
