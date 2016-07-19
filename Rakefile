require 'pg'

# TODO read from config.json?
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

def connect_db(db_config)
  PG.connect user:     db_config[:username], \
             password: db_config[:password], \
             dbname:   db_config[:database], \
             port:     db_config[:host_port], \
             host:     'localhost'
end

def container_running?(name)
  containers = `docker ps | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

def container_exists?(name)
  containers = `docker ps -a | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

begin
  $db = connect_db $config[:db]
rescue Exception => e
  puts 'No database connection'
  puts e.message if ENV['DEBUG']
  $db = nil
end

namespace :dev do
  namespace :db do
    def existing_migrations
      Dir.glob('resources/migrations/*.up.sql').map do |f|
        matches = /^(\d+)-([a-z-]+)\.up\.sql$/.match File.basename(f)
        if matches
          {
            :id   => matches[1],
            :name => matches[2].gsub(/-/, ' '),
            :file => f
          }
        end
      end
    end

    def pending_migrations
      run = $db.query('select id from schema_migrations')
      run_ids = run.map { |r| r['id'] }.sort
      existing_migrations.reject { |e| run_ids.include? e[:id] }
    end

    desc 'Run the database container'
    task :run do
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
  --name #{container_name} -e POSTGRES_PASSWORD=#{password} -d -p 127.0.0.1:#{port}:5432 #{image}
EOT
        sh cmd
      end
    end

    desc 'Run a psql shell'
    task :shell do
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

    desc 'Show pending migrations'
    task :show_pending do
      pending_migrations.each do |m|
        printf "%-16s%s\n" % [m[:id], m[:name]]
      end
    end

    desc 'Run pending migrations'
    task :run_migrations do
      if not pending_migrations.empty?
        sh 'lein migratus'
      else
        puts 'No migrations to run'
      end
    end

    desc 'Create a new migration'
    task :new_migration, [:name] do |_, args|
      fail 'Missing required name' if not args[:name]
      sh "lein migratus create #{args[:name]}"
      new_file = pending_migrations.sort_by { |m| m[:id] }.first[:file]
      puts "Created #{new_file}"
    end

    desc 'Import links from a file'
    task :import, [:file, :source, :tag] do |t, args|
      file = args[:file] or fail 'Missing file'
      source = args[:source] or fail 'Missing source'
      tag = args[:tag] or nil

      # TODO config
      host = 'localhost'
      port = 3000
      url = "http://#{host}:#{port}/links/#{source}"
      if not tag.nil?
        url += "?tag=#{tag}"
      end

      # TODO cmd_for command + hash of option keys and vals
      cmd = <<-EOT
curl \
  -XPOST \
  -H 'Content-Type: application/json' \
  --data @#{file} \
  #{url}
EOT
      sh cmd
    end
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
