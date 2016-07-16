$config = {
  :db => {
    :container_name => 'ppd-db',
    :image          => 'postgres',
    :username       => 'postgres',
    :password       => 'secretlol',
    :database       => 'ppd',
    :host_port      => 6432
  },
  :services => {
    :server   => 'lein run',
    :figwheel => 'lein figwheel'
  }
}

def container_running?(name)
  containers = `docker ps | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

def container_exists?(name)
  containers = `docker ps -a | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

namespace :dev do
  desc 'Run the database container'
  task :run_db do
    container_name = $config[:db][:container_name]
    password = $config[:db][:password]
    image = $config[:db][:image]
    port = $config[:db][:host_port]

    case
    when container_running?(container_name)
      puts "Container '#{container_name}' already running"
    when container_exists?(container_name)
      sh "docker start #{container_name}"
    else
      cmd = <<-EOT
docker run \
  --name #{container_name} -e POSTGRES_PASSWORD=#{password} -d -p #{port}:5432 #{image}
EOT
      sh cmd
    end
  end

  desc 'Run a psql shell'
  task :dbshell do
    container_name = $config[:db][:container_name]
    password = $config[:db][:password]
    username = $config[:db][:username]
    image = $config[:db][:image]

    raise 'Database container not running' unless container_running?(container_name)

    cmd = <<-EOT
docker run \
  -e PGPASSWORD=#{password} \
  -it --rm --link #{container_name}:#{image} #{image} psql -h postgres -U #{username}
EOT
    sh cmd
  end

  desc 'Start services'
  task :start do
    commands = $config[:services].values
    tmux_commands = commands[0..-2].map { 'tmux split' }
    tmux_commands << commands.map.with_index do |c, i|
      "tmux send-keys -t #{(i + 1)} '#{c}' Enter"
    end

    start_command = tmux_commands.join ' && '
    sh start_command
  end
end
