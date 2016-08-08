# puff-puff-dash

## Prerequisites

+ Docker
+ docker-compose 1.6+ (for deployment)
+ Ruby 1.9.3+ (for Rake)
+ Leiningen 2.0

## Running

### Local Development

Since I am typically in tmux, the Rake task `dev:start` is
configured to split the current tmux window and run both
the backend and frontend services. If you are not using
tmux or would like to start only one of the services,
the following commands will do that:

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
    + target/uberjar/*.jar
    + docker-compose.yml
    + Dockerfile
    + Dockerfile-db
    + Rakefile
    + resources/migrations/
    + resources/bin/
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

## License

Copyright © 2016 FIXME
