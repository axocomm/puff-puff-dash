$config = {
  :db => {
    :container_name => 'ppd-db',
    :image          => 'postgres',
    :username       => 'postgres',
    :password       => 'secretlol',
    :database       => 'ppd'
  }
}

def container_running?(name)
  containers = `docker ps | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

namespace :dev do
  desc 'Run the database container'
  task :run_db do
    container_name = $config[:db][:container_name]
    password = $config[:db][:password]
    image = $config[:db][:image]

    if container_running?(container_name)
      puts 'Container already running'
    else
      cmd = <<-EOT
docker run \
  --name #{container_name} -e POSTGRES_PASSWORD=#{password} -d #{image}
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
end
