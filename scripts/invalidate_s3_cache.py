import sys
import boto3
from time import time

AWS_ACCESS_KEY = sys.argv[1]
AWS_SECRET_ACCESS_KEY = sys.argv[2]
AWS_CF_DISTRIBUTION_ID = sys.argv[3]
AWS_PATH = sys.argv[4]  # such as releases/
PACKAGE_NAME = sys.argv[5]  # such as com.bandyer.library
PACKAGE_VERSION = sys.argv[6]  # such as 1.0

aws_package_path = PACKAGE_NAME.replace(".", "/")

if AWS_PATH[0] == '/':
    AWS_PATH = AWS_PATH[1:]

if AWS_PATH.endswith('/'):
    AWS_PATH = AWS_PATH[:-1]

root_path = AWS_PATH + "/" + aws_package_path + "/"

package_split = PACKAGE_NAME.split('.')

foldersToInvalidate = [ '/?list-type=2&delimiter=/&prefix='+ AWS_PATH +'/&max-keys=50',
                         '/?list-type=2&delimiter=/&prefix='+ AWS_PATH + '/' + package_split[0] + '/&max-keys=50',
                         '/?list-type=2&delimiter=/&prefix='+ AWS_PATH + '/' + package_split[0] + '/' + package_split[1] +'/&max-keys=50',
                         '/?list-type=2&delimiter=/&prefix='+ root_path +'&max-keys=50', # invalidate necessary for website indexing
                         '/?list-type=2&delimiter=/&prefix='+ root_path + PACKAGE_VERSION + '/&max-keys=50', # invalidate necessary for website indexing
                        "/" +  root_path + "maven-metadata.*",
                        "/" +  root_path + PACKAGE_VERSION + '/*']

client = boto3.client('cloudfront', aws_access_key_id=AWS_ACCESS_KEY, aws_secret_access_key=AWS_SECRET_ACCESS_KEY)

response = client.create_invalidation(
    DistributionId=AWS_CF_DISTRIBUTION_ID,
    InvalidationBatch={
        'Paths': {
            'Quantity': len(foldersToInvalidate),
            'Items': foldersToInvalidate
        },
        'CallerReference': str(time()).replace(".", "")
    }
)
